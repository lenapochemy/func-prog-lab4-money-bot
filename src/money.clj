(ns money
  (:require [db :refer [add-category-to-db add-income-to-db add-user-to-db add-waste-to-db
                        delete-category-from-db find-all-categories-in-db
                        find-category-in-db find-user-in-db find-wastes-by-category-in-db
                        update-category-in-db delete-all-wastes-from-db]]
            [clojure.string :as str]
            [java-time.api :as jt]))

(defn help []
  (apply str
         "Привет, это бот для контроля за своими финансами, сюда можно вносить свои траты по категориям и смотреть статистику за последнюю неделю\n"
         "/start Чтобы начать пользоваться ботом\n"
         "/help Чтобы узнать все что умеет бот\n"
         "/my_category Посмотреть свои категории\n"
         "/add_new_category <категория> Чтобы добавить новую категорию, категория должна быть одним словом\n"
         "/update_category <старая категория> <новая категория> Чтобы заменить старую категорию на новую\n"
         "/delete_category <категория> Чтобы удалить категорию и все траты этой категории\n"
         "/add_income <доход> Чтобы добавить доход\n"
         "/my_money Чтобы узнать остаток средств\n"
         "/add_spending <категория> <трата> Чтобы добавить трату в категорию\n"
         "/delete_spendings Чтобы удалить все траты\n"
         "/stat_category <категория> Чтобы получить статистику по категории\n"
         "/stat Чтобы узнать статистику по всем категория\n"))

(defn split-input [input]
  (rest (str/split input #"\s+")))

(defn add-user [username first_name chat_id]
  (if (empty?  (find-user-in-db username))
    (do
      (add-user-to-db username first_name chat_id)
      (apply str "Привет, " first_name ", теперь ты пользователь бота, держи инструкцию."))
    (apply str "Привет, " first_name ", мы уже знакомы, напоминаю как пользоваться ботом.")))

(defn add-category [username input]
  (let [user-in-db (find-user-in-db username)
        category (first (split-input input))]
    (cond (empty? user-in-db) "Неизвестный пользователь"
          (empty? category) "Категория не может быть пустой"
          (empty? (find-category-in-db (:id user-in-db) category)) (do
                                                                     (add-category-to-db (:id user-in-db) category)
                                                                     (apply str "Категория \"" category  "\" добавлена, можешь пользоваться."))
          :else "Такая категория уже есть.")))

(defn update-category [username input]
  (let [user-in-db (find-user-in-db username)
        old-category (first (split-input input))
        new-category (second (split-input input))]
    (cond (empty? user-in-db) "Неизвестный пользователь"
          (or (nil? old-category) (nil? new-category)) "Категория не может быть пустой"
          (= old-category new-category) "Новая категория полностью совпадает со старой"
          (empty? (find-category-in-db (:id user-in-db) old-category)) (apply str "Категории \"" old-category "\" не существует.")
          (empty? (find-category-in-db (:id user-in-db) new-category)) (do
                                                                         (update-category-in-db (:id user-in-db) old-category new-category)
                                                                         (apply str "Категория \"" old-category "\" изменена на категорию \"" new-category  "\", можешь пользоваться."))
          :else "Такая категория уже есть.")))

(defn delete-category [username input]
  (let [user-in-db (find-user-in-db username)
        category (first (split-input input))]
    (cond (empty? user-in-db) "Неизвестный пользователь"
          (empty? category) "Категория не может быть пустой"
          (empty? (find-category-in-db (:id user-in-db) category)) (apply str "Категории \"" category "\"  не существует.")
          :else (do
                  (delete-category-from-db (:id user-in-db) category)
                  (apply str "Категория \"" category "\"  удалена.")))))

(defn filter-categories [categories]
  (apply str "\n" (str/join "\n" (map :category categories))))

(defn get-categories [username]
  (let [user-in-db (find-user-in-db username)]
    (if (empty? user-in-db)
      "Неизвестный пользователь"
      (let [categories (find-all-categories-in-db (:id user-in-db))]
        (if (empty? categories)
          "У вас нет ни одной категории, используйте команду /add_new_category, чтобы добавить новую категорию"
          (apply str "Все ваши категории: "  (filter-categories categories)))))))

(defn check-balance [money]
  (when (> 0 money)
    "\nВаш баланс ушел в минус, кажется у вас проблемы"))

(defn get-money [username]
  (let [user-in-db (find-user-in-db username)]
    (cond (empty? user-in-db) "Неизвестный пользователь"
          :else (apply str "Ваш остаток средств: " (str (:money user-in-db)) "p." (check-balance (:money user-in-db))))))

(defn add-income [username input]
  (let [user-in-db (find-user-in-db username)
        income (first (split-input input))]
    (cond (empty? user-in-db) "Неизвестный пользователь."
          (empty? income) "Доход не может быть пустым."
          (not (number? (parse-double income))) "Доход должен быть целым числом или числом с точкой."
          (neg? (parse-double income)) "Доход должен быть положительным числом."
          (> 1 (parse-double income)) "Доход не может быть меньше нуля."
          :else (do
                  (add-income-to-db (:id user-in-db) (parse-double income))
                  (apply str "Доход добавлен.\n"
                         (get-money username))))))

(defn add-waste [username input]
  (let [user-in-db (find-user-in-db username)
        category (first (split-input input))
        waste (second (split-input input))]
    (cond (empty? user-in-db) "Неизвестный пользователь."
          (empty? category) "Категория не может быть пустой."
          (double? (parse-double category)) "Категория должна быть строкой."
          (empty? waste) "Трата не может быть пустой."
          (not (number? (parse-double waste))) "Трата должна быть целым числом или числом с точкой."
          (neg? (parse-double waste)) "Трата должна быть положительным числом."
          (> 1 (parse-double waste)) "Трата не может быть меньше рубля."
          :else (let [category-in-db (first (find-category-in-db (:id user-in-db) category))]
                  (if
                   (nil? category-in-db) (apply str "Категории \"" category "\" не существует.")
                   (do
                     (add-waste-to-db (:id category-in-db) user-in-db (parse-double waste))
                     (apply str "Трата добавлена.\n" (get-money username))))))))

(def days-of-week {"MONDAY" "понедельник"
                   "TUESDAY" "вторник"
                   "WEDNESDAY" "среда"
                   "THURDAY" "четверг"
                   "FRIDAY" "пятница"
                   "SATURDAY" "суббота"
                   "SUNDAY" "воскресенье"})

(defn filter-wastes [wastes]
  (str/join "\n" (map (fn [w]
                        (apply str (:waste w) " p.  " (get days-of-week (:day w)) ", " (jt/format "dd.MM.yyyy" (jt/local-date (:date w))))) wastes)))

(defn last-monday []
  (loop [now (jt/local-date)]
    (if (jt/monday? now)
      now
      (recur (jt/- now (jt/days 1))))))

(defn filter-stat-by-week [stat]
  (let [last-monday (last-monday)]
    (filter #(jt/after? (jt/local-date (get % :date)) last-monday) stat)))

(defn stat-by-one-category [username category]
  (let [user-in-db (find-user-in-db username)]
    (cond (empty? user-in-db) "Неизвестный пользователь."
          (empty? category) "Категория не может быть пустой."
          :else (let [category-in-db (first (find-category-in-db (:id user-in-db) category))]
                  (if
                   (nil? category-in-db) (apply str "Категории \"" category "\" не существует.")
                   (let [stat (find-wastes-by-category-in-db (:id category-in-db))]
                     (if (empty? stat)
                       (apply str "У вас нет трат в категории \"" category "\".")
                       (let [filtered-stat (filter-stat-by-week stat)]
                         (apply str "Траты из категории \"" category "\" за последную неделю:\n\n" (filter-wastes filtered-stat)
                                "\n\nБыло сделано " (count filtered-stat) " покупок."
                                "\nВсего потрачено: " (apply + (map (fn [w] (:waste w)) filtered-stat)) " p.")))))))))

(defn stat-by-category [username input]
  (let [user-in-db (find-user-in-db username)
        category (first (split-input input))]
    (cond (empty? user-in-db) "Неизвестный пользователь."
          (empty? category) "Категория не может быть пустой."
          :else (stat-by-one-category username category))))

(defn stat [username]
  (let [user-in-db (find-user-in-db username)]
    (if (empty? user-in-db) "Неизвестный пользователь."
        (let [categories (find-all-categories-in-db (:id user-in-db))]
          (if (empty? categories)
            "У вас нет категорий и трат"
            (str/join "\n\n" (map (fn [cat] (stat-by-one-category username (:category cat))) categories)))))))

(defn delete-wastes [username]
  (println username)
  (let [user-in-db (find-user-in-db username)]
    (if (empty? user-in-db) "Неизвестный пользователь"
        (do
          (delete-all-wastes-from-db (:id user-in-db))
          "Все ваши траты удалены"))))

