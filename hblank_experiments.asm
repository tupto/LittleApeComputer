mov a VM
str a x2            ; enable tile mode
mov a BG_ENABLE
str a x2            ; turn on BG1
mov a IH
mov b interrupt_handler
str a b         ; IH = interrupt_handler
sub a xE        ; IH - xF = IE
str a x3        ; IE = VBLANK | HBLANK
sub a x2        ; IE - x2 = IME
str a x1        ; IME enabled

mov a palettes
mov b PAL
mov c 4
bl 0x0004           ; bios mem_cpy function

mov a tiles
mov b VRAM
mov c 8
bl 0x0004           ; bios mem_cpy function

loop:
    bl vblank_wait  ; wait for vblank
    b loop

interrupt_handler:
    push a-b
    
    mov a vblank_wait_flag
    str a x1        ; set vblank_wait_flag
    
    
    mov a PAL
    ldr b a
    sub b 0x7C00
    bz +4
    mov b 0x7C00
    b end
    mov b 0x1F
    
    end:
    str a b
   
    pop a-b
    ret

vblank_wait:
    push a-b
    mov a vblank_wait_flag
    ldr b a
    bz -2           ; loop ldr/bz
    str a x0        ; clear vblank_wait_flag
    pop a-b
    ret
    
vblank_wait_flag:
.data
0x0000
.enddata

palettes:
.data
; black/white
0x7C00,0x0000,0x0000,0x0000
.enddata

tiles:
.data
0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000
.enddata