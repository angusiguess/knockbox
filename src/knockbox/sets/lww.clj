;; -------------------------------------------------------------------
;; Copyright (c) 2011 Basho Technologies, Inc.  All Rights Reserved.
;;
;; This file is provided to you under the Apache License,
;; Version 2.0 (the "License"); you may not use this file
;; except in compliance with the License.  You may obtain
;; a copy of the License at
;;
;;   http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing,
;; software distributed under the License is distributed on an
;; "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
;; KIND, either express or implied.  See the License for the
;; specific language governing permissions and limitations
;; under the License.
;;
;; -------------------------------------------------------------------

(in-ns 'knockbox.sets)

(defn hash-max
  "Merge two hashes, taking the
  max value from keys in both hashes"
  [a b]
  (merge-with max a b))

(defn- lww-minus-deletes
  "Remove deletes with
  earlier timestamps
  than adds"
  [adds dels]
  (let [favor-deletes (fn [add delete] (if (>= delete add) nil add))
        no-deletes (merge-with favor-deletes
                               adds
                               (select-keys dels (keys adds)))
        no-nil (fn [a] (not= (get a 1) nil))]
    (map #(get % 0)
         (filter no-nil no-deletes))))

(deftype LWWSet [^IPersistentMap adds
                 ^IPersistentMap dels]

  IPersistentSet
  (disjoin [this k]
    (let [now (System/nanoTime)]
      (LWWSet. adds
               (assoc dels k now))))

  (get [this k]
    (if (get adds k)
      (if (get dels k)
        (if (> (get adds k) (get dels k))
          k
          nil)
        k)
      nil))

  (seq [this]
    (seq (lww-minus-deletes adds dels)))

  (count [this]
    (count (seq this)))

  clojure.lang.IPersistentCollection
  (cons [this k]
    (let [now (System/nanoTime)]
      (LWWSet.
        (assoc adds k now)
        dels)))

  (empty [this]
    (LWWSet. {} {}))

  (equiv [this other]
    (.equals this other))

  IObj
  (meta [this]
    (.meta ^IObj adds))

  (withMeta [this m]
    (LWWSet. (.withMeta ^IObj adds m)
             dels))

  Object
  (hashCode [this]
    (hash (set (seq this))))

  (equals [this other]
    (or (identical? this other)
        (and (instance? Set other)
             (let [^Set o (cast Set other)]
               (and (= (count this) (count o))
                    (every? #(contains? o %) (seq this)))))))

  (toString [this]
    (.toString (set (seq this))))

  Set
  (contains [this k]
    (boolean (get this k)))

  (containsAll [this ks]
    (every? identity (map #(contains? this %) ks)))

  (size [this]
    (count this))

  (isEmpty [this]
    (= 0 (count this)))

  (toArray [this]
    (RT/seqToArray (seq this)))

  (toArray [this dest]
    (reduce (fn [idx item]
              (aset dest idx item)
              (inc idx))
            0, (seq this))
    dest)

  ;; this is just here to mark
  ;; the object as serializable
  Serializable

  IFn
  (invoke [this k]
    (get this k))

  java.lang.Iterable
  (iterator [this]
    (clojure.lang.SeqIterator. (seq this)))

  Resolvable 
  (resolve [this other]
    (let [new-adds (hash-max adds (.adds other))
          new-dels (hash-max dels (.dels other))]
      (LWWSet. new-adds new-dels)))

  (gc [this gc-max-seconds gc-max-items]
    this)

  cheshire.custom/JSONable
  (to-json [this jsongen]
    (let [m {:type "lww-e-set"}
          rfn (fn [acc elem]
                (let [a (clojure.core/get adds elem)
                      d (clojure.core/get dels elem)
                      ea [elem a]
                      vect (if d (conj ea d) ea)]
                  (conj acc vect)))
          elems (reduce rfn [] (clojure.set/union (keys adds)
                                                  (keys dels)))]
      (.writeRaw jsongen (cheshire.core/generate-string
                           (assoc m :e elems))))))

(defn lww
  "Creates a new `LWWSet`. This type
  uses timestamps to resolve conflicts
  between set addition/removal. Under the hood,
  two maps are used to represent addition and removal.
  The keys in the maps are values in the set,
  and values are the timestamps. For example,
  if something is added to the set, an
  entry into the `adds` map is made mapping
  from the entry => timestamp. Conflicts
  are resolved by letting the set with the most
  recent not-nil timestamp win. This means that
  if the timetstamp for deleting `:foo` is more
  recent than adding it, `:foo` won't appear
  in the set at all"
  []
  (LWWSet. {} {}))

(defmethod knockbox.core/handle-json-structure "lww-e-set"
  ;TODO
  ;need to figure out
  ;how to deal with
  ;strings vs. keywords
  [obj]
  (let [rfn (fn [[a r] [elem a-time & r-time]]
              [(if a-time (assoc a (keyword elem) a-time) a)
               (if (seq r-time) (assoc r (keyword elem) (first r-time)) r)])
        [a r] (reduce rfn [{} {}] (:e obj))]
    (LWWSet. a r)))

(defmethod print-dup knockbox.sets.LWWSet [o w]
  (.write w (str "#=(" (.getName ^Class (class o)) ". " (.adds o) " " (.dels o) ")")))
