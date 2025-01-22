(ns db
  (:require [clojure.java.jdbc :as jdbc]
            [java-time.api :as jt]))
(def db {:dbtype "postgresql"
         :dbname "money"
         :host "localhost"
         :port "5432"
         :user ""
         :password ""})

(defn find-user-in-db [username]
  (first (jdbc/find-by-keys db :users {:username username})))

(defn add-user-to-db [username first_name]
  (jdbc/insert! db :users {:username username
                           :first_name first_name
                           :money 0}))

(defn find-all-categories-in-db [user-id]
  (jdbc/find-by-keys db :categories {:user_id user-id}))

(defn find-category-in-db [user-id category]
  (jdbc/find-by-keys db :categories {:user_id user-id
                                     :category category}))

(defn add-category-to-db [user-id category]
  (jdbc/insert! db :categories {:user_id user-id
                                :category category}))

(defn update-category-in-db [user-id old-category new-category]
  (jdbc/update! db :categories {:category new-category} ["user_id = ? AND category = ?" user-id old-category]))

(defn delete-category-from-db [user-id category]
  (jdbc/delete! db :categories ["user_id = ? AND category = ?" user-id category]))

(defn add-income-to-db [user-id income]
  (let [user-from-db (jdbc/get-by-id db :users user-id)]
    (jdbc/update! db :users {:money (+ (:money user-from-db) income)} ["id = ?" user-id])))

(defn find-wastes-by-category-in-db [category-id]
  (jdbc/find-by-keys db :wastes {:category_id category-id}))

(defn add-waste-to-db [category_id user-from-db waste]
  (jdbc/insert! db :wastes {:category_id category_id
                            :waste waste
                            :date (jt/local-date)
                            :day (str (jt/day-of-week (jt/local-date)))})
  (jdbc/update! db :users {:money (- (:money user-from-db) waste)} ["id = ?" (:id user-from-db)]))

(defn delete-all-wastes-from-db [user-id]
  (let [categories (find-all-categories-in-db user-id)]
    (println categories)
    (dorun (map (fn [cat]
                  (println cat)
                  (println (:id cat))
                  (jdbc/delete! db :wastes ["category_id = ?" (:id cat)])) categories))))
