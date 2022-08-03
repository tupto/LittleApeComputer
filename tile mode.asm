mov a VM
str a x2            ; enable tile mode
mov a BG_ENABLE
str a x2            ; turn on BG1
mov a IH
mov b interrupt_handler
str a b         ; IH = interrupt_handler
sub a xE        ; IH - xF = IE
str a x1        ; IE = VBLANK
sub a x2        ; IE - x2 = IME
str a x1        ; IME enabled

mov a palettes
mov b PAL
mov c 28
bl 0x0004           ; bios mem_cpy function

mov a tiles
mov b VRAM
mov c 0x38
bl 0x0004           ; bios mem_cpy function

mov a wizard_sprites
mov b OAM
mov c 36
bl 0x0004           ; bios mem_cpy function

mov a BG1
mov b 32
mov c 16
mov d 0x0606

check_x:
    sub b x1
    bs reset_x
    b store

reset_x:
    mov b 32
    add a 31
    
check_y:
    sub c x1
    bs loop_start

store:
    str a d
    add a x1
    b check_x

loop_start:
    mov a BG1_X
loop:
    bl vblank_wait  ; wait for vblank
    ldr b a
    add b x1
    ;str a b
    add a x1
    ldr b a
    add b x1
    ;str a b
    sub a x1
    b loop

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
    
vblank_wait_flag:
.data
0x0000
.enddata

palettes:
.data
; wizards
0x8000,0x0000,0x2E58,0x4921
0x8000,0x0000,0x2E58,0x6A21
0x7FFF,0x0000,0x2E58,0x6A21
0x8000,0x0000,0x001F,0x4921
0x8000,0x0000,0x7C00,0x4921
0x8000,0x0000,0x7C1F,0x4921

;grass
0x1DEA,0x36CE,0x36D4,0x266D
.enddata

tiles:
.data
; wizards
0x0005,0x001A,0x0016,0x0041,0x0006,0x0015,0x0003,0x0003
0x5540,0xAA50,0xAAA5,0xAAAA,0xAA55,0x55FF,0xFFFF,0xFDF7
0x0000,0x003C,0x00EB,0x54EB,0x00EB,0x00FC,0x00FC,0x00F0
0x0006,0x001A,0x006A,0x006A,0x006A,0x001A,0x001A,0x006A
0xFFFF,0x3C0F,0x8053,0xA000,0xA80A,0xAAAA,0x5A96,0xA6A9
0x40F0,0x90F0,0xA5F0,0xAAF0,0xA9F0,0xA4F0,0xA4F0,0x95F0

;grass
0x0000,0x0408,0x1000,0x0000,0x0803,0x0233,0x000C,0x0000
.enddata

wizard_sprites:
.data
0xA020,0x0000
0xA028,0x0101
0xA030,0x0302
0xA820,0x0003
0xA828,0x0204
0xA830,0x0005

0xA850,0x8000
0xA848,0x8101
0xA840,0x8402
0xB050,0x8003
0xB048,0x8204
0xB040,0x8005

0xB422,0x0000
0xB42A,0x0101
0xB432,0x0502
0xBC22,0x0003
0xBC2A,0x0204
0xBC32,0x0005
.enddata