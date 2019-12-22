(ns beetleman.nipper.data
  (:require [clojure.string :as string])
  (:import [java.util UUID]))


(defn random-str [size]
  (string/join (repeatedly size #(rand-int 10))))


(defn random-map
  [size value-fn]
  (reduce (fn [acc [k v]] (assoc acc k v))
          {}
          (repeatedly size (fn [] [(UUID/randomUUID) (value-fn)]))))


(defn random-vector [size value-fn]
  (reduce conj [] (repeatedly size value-fn)))


(defn random-set [size value-fn]
  (reduce conj #{} (repeatedly size value-fn)))


(defn generate
  [size]
  (random-map size
              (fn []
                (let [f (rand-nth [random-map random-vector])]
                  (f (rand-int 100) #(random-str (rand-int 100)))))))
