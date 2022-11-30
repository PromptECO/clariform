;; Missing 'begin' form in original

(define-read-only (half (digit int))
  (asserts! (<= 0 digit 9))
  (/ digit 2))
