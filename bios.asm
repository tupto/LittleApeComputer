;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                       ;;
;;       bios.asm        ;;
;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

b soft_reset
b interrupt_vector
b mem_cpy
b divide
b modulo
b sine
b cosine
b arctan
b arctan2


soft_reset:
    mov sp xBFFF    ; 
    mov pc x1000    ; SP and PC to default
    
interrupt_vector:
    push a-c
    mov a IH
    ldr c a
    sub a xD        ; IF = IH - xD
    ldr b a         ; load current interrupt flags in b
    
    bl c            ; branch to user interrupt handler
    
    ldr c a         ; load current interrupt flags into c
    xor b c         ; clear old request flags but keep any new that may have occured
    str a b         ; store updated request flags
    sub a x2        ; IRQ = IF - x2
    str a x0        ; turn off interrupt request mode (we're no longer handling a request)
    pop a-c
    ret

;a = src, b = dest, c = len
mem_cpy:
    push a-d
    add a c
    add b c
    mem_cpy_loop:
        sub c x1
        bs mem_cpy_end
        sub a x1
        sub b x1
        ldr d a
        str b d
        b mem_cpy_loop
        mem_cpy_end:
            pop a-d
            ret

divide:
    push b-e
    push 
    pop c-e
    ret
modulo:
    ret
sine:
    ret
cosine:
    ret
arctan:
    ret
arctan2:
    ret