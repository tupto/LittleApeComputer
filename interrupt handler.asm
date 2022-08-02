mov a IH
mov b interrupt_handler
str a b         ; IH = interrupt_handler
sub a xE        ; IH - xF = IE
str a x1        ; IE = VBLANK
sub a x2        ; IE - x2 = IME
str a x1        ; IME enabled

loop:
    bl vblank_wait  ; wait for vblank

    mov a VRAM
    add c x01
    and c x7f

    fill:
        mov b c
        lsl b x8
        add b c
        add b x1
        str a b
        mov b c
        sub b x7F
        bs inc
        mov c x00
    inc:
        add c x2
        add a x1
        bz loop
        b fill

interrupt_handler:
    push a
    
    mov a vblank_wait_flag
    str a x1        ; set vblank_wait_flag
    
    pop a
    ret

vblank_wait:
    push a-b
    mov a vblank_wait_flag
    ldr b a
    bz -2           ; loop ldr/bz
    str a x0        ; clear vblank_wait_flag
    pop a-b
    ret

vblank_wait_flag:   ; using label as a memory location for a variable
    hlt