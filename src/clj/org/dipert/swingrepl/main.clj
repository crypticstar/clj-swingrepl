(ns org.dipert.swingrepl.main
  "Swing Clojure REPL using BeanShell's JConsole"
  (:require clojure.main)
  (:import (javax.swing JFrame))
  (:gen-class))

(def ^{:doc "Formatted Clojure version string"
       :private true}
     clj-version
     (apply str (interpose \. (map *clojure-version* [:major :minor :incremental]))))

(def ^{:doc "Default REPL display options"
       :private false}
     default-opts
     {:width 600
      :height 400
      :title (str "Clojure " clj-version)
      :on-close JFrame/DISPOSE_ON_CLOSE})

(defn make-repl-jframe
  "Displays a JFrame with JConsole and attached REPL."
  ([] (make-repl-jframe {}))
  ([optmap]
     (let [options (merge default-opts optmap)
	   {:keys [title width height on-close]} options
	   jframe (doto (JFrame. title)
		    (.setSize width height)
		    (.setDefaultCloseOperation on-close)
		    (.setLocationRelativeTo nil)
		    (.setVisible true))]
	(let [console (bsh.util.JConsole.)]
	   (doto (.getContentPane jframe)
	     (.setLayout (java.awt.BorderLayout.))
	     (.add console))
	   (doto jframe
	     (.pack)
	     (.setSize width height))
	   (binding [*out* (java.io.OutputStreamWriter. (.getOut console))
		     *in*  (clojure.lang.LineNumberingPushbackReader. (.getIn console))
              *err* (.getOut console)]
	     (.start (Thread. (bound-fn [] (clojure.main/main)))))))))


;; Debug swing macro
;
; Can't take credit for the debug macro, came from here:
; http://gist.github.com/252421
; Inspired by George Jahad's version: http://georgejahad.com/clojure/debug-repl.html
(defmacro local-bindings
  "Produces a map of the names of local bindings to their values."
  []
  (let [symbols (map key @clojure.lang.Compiler/LOCAL_ENV)]
    (zipmap (map (fn [sym] `(quote ~sym)) symbols) symbols)))

(declare *locals*)
(defn eval-with-locals
  "Evals a form with given locals. The locals should be a map of symbols to
  values."
  [locals form]
  (binding [*locals* locals]
    (eval
      `(let ~(vec (mapcat #(list % `(*locals* '~%)) (keys locals)))
         ~form))))

(defmacro make-dbg-repl-jframe
  "Displays a JFrame with JConsole and attached REPL. The frame has the context
  from wherever it has been called, effectively creating a debugging REPL.

  Usage:

    (use 'org.dipert.swingrepl.main)
    (defn foo [a] (+ a 5) (make-dbg-repl-jframe {}) (+ a 2))
    (foo 3)

  This will pop up the debugging REPL, you should be able to access the var 'a'
  from the REPL.
  "
  ([] `(make-dbg-repl-jframe {}))
  ([optmap]
  `(let [opts# (merge default-opts ~optmap)
         jframe# (doto (JFrame. (:title opts#))
                   (.setSize (:width opts#) (:height opts#))
                   (.setDefaultCloseOperation (:on-close opts#))
                   (.setLocationRelativeTo nil)
                   (.setVisible true))]
     (let [console# (bsh.util.JConsole.)]
       (doto (.getContentPane jframe#)
         (.setLayout (java.awt.BorderLayout.))
         (.add console#))
       (doto jframe#
         (.pack)
         (.setSize (:width opts#) (:height opts#)))
       (binding [*out* (java.io.OutputStreamWriter. (.getOut console#))
                 *in*  (clojure.lang.LineNumberingPushbackReader. (.getIn console#))
                 *err* (.getOut console#)]
         (.start (Thread. (bound-fn []
                                    (clojure.main/repl
                                      :prompt #(print "dr => ")
                                      :eval (partial eval-with-locals (local-bindings)))))))))))


(defn -main
  [& args]
  (make-repl-jframe {:on-close JFrame/EXIT_ON_CLOSE}))

