(ns bot
  (:require [clojure.string :as str]
            [morse.handlers :as h]
            [morse.polling :as p]
            [morse.api :as t]
            [money :refer [add-category add-income add-user add-waste delete-category delete-wastes
                           get-categories get-money help stat stat-by-category update-category]]
            [clojure.core.async :as async]))

(def token "")

#_:clj-kondo/ignore
(h/defhandler handler
  (h/command-fn "start"
                (fn [{{id :id :as chat} :chat}]
                  (println "Bot joined new chat: " chat)
                  (t/send-text token id (add-user (:username chat) (:first_name chat) (:id chat)))
                  (t/send-text token id (help))))

  (h/command-fn "help"
                (fn [{{id :id :as chat} :chat}]
                  (println "Help was requested in " chat)
                  (t/send-text token id (help))))

  (h/command-fn "add_new_category"
                (fn [{{id :id} :chat :as chat}]
                  (println "Add new category was requested in " chat)
                  (t/send-text token id (add-category (:username (:chat chat)) (:text chat)))))
  (h/command-fn "delete_category"
                (fn [{{id :id} :chat :as chat}]
                  (println "Delete category was requested in " chat)
                  (t/send-text token id (delete-category (:username (:chat chat)) (:text chat)))
                  (t/send-text token id (get-categories (:username (:chat chat))))))
  (h/command-fn "update_category"
                (fn [{{id :id} :chat :as chat}]
                  (println "Update category was requested in " chat)
                  (t/send-text token id (update-category (:username (:chat chat)) (:text chat)))))
  (h/command-fn "my_category"
                (fn [{{id :id :as chat} :chat}]
                  (println "Get all categories was requested in " chat)
                  (t/send-text token id (get-categories (:username chat)))))

  (h/command-fn "add_income"
                (fn [{{id :id} :chat :as chat}]
                  (println "Add income was requested in " chat)
                  (t/send-text token id (add-income (:username (:chat chat)) (:text chat)))))
  (h/command-fn "my_money"
                (fn [{{id :id :as chat} :chat}]
                  (println "Get money was requested in " chat)
                  (t/send-text token id (get-money (:username chat)))))

  (h/command-fn "add_spending"
                (fn [{{id :id} :chat :as chat}]
                  (println "Add waste was requested in " chat)
                  (t/send-text token id (add-waste (:username (:chat chat)) (:text chat)))))
  (h/command-fn "delete_spendings"
                (fn [{{id :id :as chat} :chat}]
                  (println "Delete waste was requested in " chat)
                  (t/send-text token id (delete-wastes (:username chat)))))

  (h/command-fn "stat_category"
                (fn [{{id :id} :chat :as chat}]
                  (println "Stat by category was requested in " chat)
                  (t/send-text token id (stat-by-category (:username (:chat chat)) (:text chat)))))

  (h/command-fn "stat"
                (fn [{{id :id :as chat} :chat}]
                  (println "Get stat was requested in " chat)
                  (t/send-text token id (stat (:username chat)))))

  (h/message-fn
   (fn [{{id :id} :chat :as message}]
     (println "Intercepted message: " message)
     (t/send-text token id "Я не знаю что с этим делать."))))

(defn -main []
  (when (str/blank? token)
    (println "Need token")
    (System/exit 1))
  (println "Starting the my-money-bot")
  #_:clj-kondo/ignore
  (async/<!! (p/start token handler)))