;; Original is valid with correctly balanced parens
;; but having malformed layout and indentation.
(define-read-only (plus
                  (n int)
       )
  (let (
    (value (+ n 1))
          )
    value
    )
 )