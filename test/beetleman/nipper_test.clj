(ns beetleman.nipper-test
  (:require [beetleman.nipper :as sut]
            [beetleman.nipper.data :as data]
            [clojure.test :as t]))


(t/deftest dump!-load!-test

  (let [path ".test/dump-load-test-1"
        m    (data/generate 4 4)
        d    (with-meta (data/generate 100) m)]
    (t/is (nil?
           (sut/dump! path d 3)))
    (let [loaded (sut/load! path)]
      (t/testing "d -> dump! -> load! -> d"
                 (t/is (= d
                          loaded)))
      (t/testing "metadata survive"
                 (t/is (= m
                          (meta loaded)))))))
