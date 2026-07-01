(ns mise.catalog
  "Product / SKU / Catalog model (pure EDN) + query functions.

  A Catalog is a map {:products [...] :categories {...}}. A Product is
  {:id :brand :name :category :description :images :variants [SKU...]}. A SKU
  (variant) is {:id :product-id :name :options {...} :price Price}. Prices are
  delegated to mise.pricing (a Price is {:amount <number> :currency \"JPY\"}).

  Portable .cljc, zero host effects. The catalog is plain data the host loads
  (from a fixture, D1, an API, …); this namespace only shapes and queries it."
  (:refer-clojure :exclude [filter])
  (:require [clojure.string :as str]))

(defrecord Product [id brand name category description images variants])
(defrecord SKU [id product-id name options price])
(defrecord Catalog [products categories])

(defn product?
  "Loose duck-typed predicate so plain maps from EDN/JSON count as products."
  [x] (and (map? x) (:id x) (:name x)))

(defn sku? [x] (and (map? x) (:id x) (:price x)))

(defn catalog
  "Build a Catalog from a seq of products (and optional category tree)."
  ([products]
   (catalog products nil))
  ([products categories]
   (->Catalog (vec products) (or categories {}))))

(defn product-by-id
  "Find a product by :id in a catalog (or seq of products)."
  [src id]
  (let [products (if (map? src) (:products src) src)]
    (some #(when (= (:id %) id) %) products)))

(defn products-by-category
  "Products whose :category equals (or descends from) the given category id."
  [src cat-id]
  (let [products (if (map? src) (:products src) src)]
    (filterv #(= (:category %) cat-id) products)))

(defn search
  "Naive case-insensitive substring search over product :name/:description."
  [src q]
  (let [products (if (map? src) (:products src) src)
        ql (some-> q str str/lower-case)]
    (if (str/blank? ql)
      (vec products)
      (filterv (fn [p]
                 (let [n (some-> (:name p) str str/lower-case)
                       d (some-> (:description p) str str/lower-case)]
                   (or (and n (str/includes? n ql))
                       (and d (str/includes? d ql)))))
               products))))

(defn sku-by-id
  "Find a SKU by id within a product (or catalog+product-id)."
  ([product sku-id]
   (some #(when (= (:id %) sku-id) %) (:variants product)))
  ([src product-id sku-id]
   (when-let [p (product-by-id src product-id)]
     (sku-by-id p sku-id))))

(defn default-sku
  "The first variant of a product (or the product itself if no variants)."
  [product]
  (or (first (:variants product))
      (when (:price product) product)))

;; ---------------------------------------------------------------------------
;; category tree (parent/child hierarchy)
;; ---------------------------------------------------------------------------

(defn category-tree
  "Build a {parent → #{children}} map from a Catalog's :categories (which may
  be a flat map of {id → label} or already a tree). v1: categories are flat;
  this returns an empty tree (host app supplies the hierarchy). When the catalog
  :categories map has entries shaped {id → {:label :parent}}, builds the tree."
  [catalog']
  (let [cats (:categories catalog' {})]
    (reduce (fn [tree [id spec]]
              (if (map? spec)
                (let [parent (:parent spec)]
                  (if parent
                    (update tree parent (fnil conj #{}) id)
                    tree))
                tree))
            {} cats)))

(defn child-categories
  "Direct children of a category id, given a category-tree."
  [tree cat-id]
  (get tree cat-id #{}))

(defn descendant-categories
  "All descendants of a category id (recursive). Returns a set."
  [tree cat-id]
  (loop [queue [cat-id] found #{}]
    (if-let [c (first queue)]
      (let [children (child-categories tree c)
            new-children (remove found children)
            found' (into found children)]
        (recur (concat (rest queue) new-children) found'))
      found)))

(defn products-in-category
  "Products in a category AND all its descendants (given a category-tree).
  For a flat catalog (no tree), falls back to direct :category match."
  ([src cat-id]
   (products-in-category src cat-id {}))
  ([src cat-id tree]
   (let [descendants (descendant-categories tree cat-id)
         all-cats (conj descendants cat-id)
         products (if (map? src) (:products src) src)]
     (filterv #(contains? all-cats (:category %)) products))))
