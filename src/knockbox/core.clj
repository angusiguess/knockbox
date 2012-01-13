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

(ns knockbox.core
  (:refer-clojure :exclude [resolve])
  (:require [knockbox.resolvable])
  (:require (cheshire core custom)))

(defn resolve [coll]
  (reduce knockbox.resolvable/resolve coll)) 

(defn to-json [obj]
  (cheshire.custom/encode obj))

(defmulti handle-json-structure
  "Return an object from
  a parsed json representation
  of an object. Useful when the json
  representation of an object is different
  from the internal representation"
  :type)

(defn from-json [json-string]
  (let [parsed (cheshire.core/parse-string json-string true)]
    (handle-json-structure parsed)))
