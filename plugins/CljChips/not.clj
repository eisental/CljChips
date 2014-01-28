(on input [state idx]
  (when (< idx ($ outputlen))
    ($ write (not state) idx)))
