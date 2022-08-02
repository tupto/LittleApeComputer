mov a VM
str a x1
mov a IH
mov b interrupt_handler
str a b         ; IH = interrupt_handler
sub a xE        ; IH - xF = IE
str a x1        ; IE = VBLANK
sub a x2        ; IE - x2 = IME
str a x1        ; IME enabled

mov c x0
mov d x0
mov e x0

b +2

loop:
    bl vblank_wait  ; wait for vblank
    mov a VRAM
    mov b x3

    bl inc_color

    fill:
        mov b c
        lsl b x5
        add b d
        lsl b x5
        add b e
        
        str a b
        
        bl inc_color
        bl inc_color
    inc:
        add a x1
        bz loop
        b fill
    inc_color:
        push b
        add c x1
        mov b c
        sub b x20
        bs inc_color_end
        mov c x00
        
        add d x1
        mov b d
        sub b x20
        bs inc_color_end
        mov d x00
        
        add e x1
        mov b e
        sub b x20
        bs inc_color_end
        mov e x00
    inc_color_end:
        pop b
        ret

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