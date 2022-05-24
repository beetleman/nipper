#  Nipper

[![CircleCI](https://circleci.com/gh/beetleman/nipper.svg?style=svg)](https://circleci.com/gh/beetleman/nipper)

`nipper` can be used for saving really big HashMaps to file system (used in production for something like ~64GB)
Most libs which can save data structure to file system make it by prepare another big bytearray in memory
so you need more memory to make dump.

`nipper` use [nippy](https://github.com/ptaoussanis/nippy) for data format but dump HashMap to filesystem
in chunks, by default every chunk is 5% of whole data structure.

## Usage

```clojure

(require '[beetleman.nipper :as nipper])

;; generate some data
(def big-data-structure (into {}
                              (map (fn [x]
                                     [(keyword "big-data" (str "k-" x))
                                      (range (rand-int 1000))]))
                              (range 1000)))


;; dump data to filesystem
(nipper/dump! "testing" big-data-structure)

;; load data from filesystem
(def big-data-structure-loaded (nipper/load! "testing"))


(= big-data-structure-loaded big-data-structure)
;; => true

```

data format on filesystem:

```bash
> tree testing
testing/
├── 0.npy
├── 10.npy
├── 11.npy
├── 12.npy
├── 13.npy
├── 14.npy
├── 15.npy
├── 16.npy
├── 17.npy
├── 18.npy
├── 19.npy
├── 1.npy
├── 2.npy
├── 3.npy
├── 4.npy
├── 5.npy
├── 6.npy
├── 7.npy
├── 8.npy
├── 9.npy
├── index.npy
└── meta.npy
```
