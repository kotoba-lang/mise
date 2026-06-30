(ns mise.inventory-test
  (:require [clojure.test :refer [deftest is testing]]
            [mise.inventory :as inventory]))

(def stk (inventory/stock {"ph-m" 5 "ph-l" 0}))

(deftest on-hand-and-available-test
  (is (= 5 (inventory/on-hand stk "ph-m")))
  (is (= 0 (inventory/on-hand stk "ph-l")))
  (is (inventory/available? stk "ph-m" 3))
  (is (not (inventory/available? stk "ph-m" 10)))
  (is (not (inventory/available? stk "ph-l" 1)))
  (is (not (inventory/available? stk "absent" 1))))

(deftest reserve-and-restock-test
  (is (= 2 (inventory/on-hand (inventory/reserve stk "ph-m" 3) "ph-m")))
  (is (= 0 (inventory/on-hand (inventory/reserve stk "ph-m" 99) "ph-m"))) ; clamped at 0
  (is (= 8 (inventory/on-hand (inventory/restock stk "ph-m" 3) "ph-m"))))

(deftest reserve-many-test
  (let [s (inventory/reserve-many stk [["ph-m" 2] ["ph-l" 1]])]
    (is (= 3 (inventory/on-hand s "ph-m")))
    (is (= 0 (inventory/on-hand s "ph-l")))))
