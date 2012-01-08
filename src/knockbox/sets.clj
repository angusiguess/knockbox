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

(ns knockbox.sets
  (:require [clojure.set])
  (:require [cheshire.custom])
  (:import (clojure.lang IPersistentSet IPersistentMap
                         IFn IObj RT)
           (java.util Set)
           (java.io Serializable))
  (:refer-clojure :exclude [resolve])
  (:use [knockbox.resolvable]))

(load "sets/lww")
(load "sets/two_phase")
(load "sets/or")
