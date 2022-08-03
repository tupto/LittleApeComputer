# Tupto's Little Ape Computer #

## Memory Map ##

```
0000-0EFF   BIOS
0F00-0FFF   Control registers
1000-BFFF   RAM
C000-FFFF   VRAM
```

## Control Registers ##

### Map ###

```
0F00        IME     Interrupt master enable      (0=off, 1=on)
0F01        IRQ     Interrupt request mode       (0=off, 1=on)
0F02        IE      Interrupt enable             (bit flags)
0F03        IF      Interrupt request flags      (bit flags)
0F10        IH      Interrupt handler            (function pointer)
0F20        VM      Video mode                   (see picture processing video modes)
    
```

### `0F00` - IME - Interrupt master enable ###

```
Bit     Desc
0       Enable/ disable interrupts          (0 = disable all, 1 = check IE)
1-15    Not used
```

### `0F01` - IE - Interrupt master enable ###

```
Bit     Desc
0       VBlank      (0 = disable, 1 = enable)
1       HBlank      (0 = disable, 1 = enable)
2       Keypad      (0 = disable, 1 = enable)
3-15    Not used
```

### `0F02` - IF - Interrupt request flags ###

```
Bit     Desc
0       VBlank      (0 = disable, 1 = enable)
1       HBlank      (0 = disable, 1 = enable)
2       Keypad      (0 = disable, 1 = enable)
3-15    Not used
```

## Registers ##
 - A
    - General purpose
 - B
    - General purpose
 - C
    - General purpose
 - D
    - General purpose
 - E
    - General purpose (cannot use immediate byte mode)
 - Stack Pointer
    - Points to current stack location
 - Program Counter
    - Points to program instruction
 - Flags
     - Sign (is negative)
     - Zero (is zero)
     - Carry (if last operation resulted in a carry)

## Opcodes ##

```
HLT     0       halt
ADD     1       add
SUB     2       subtract
AND     3       logical AND
OR      4       logical OR
XOR     5       logical XOR
LSL     6       logical shift left
LSR     7       logical shift right
MOV     8       move value to register
LDR     9       load register
STR     a       store register
B       b       branch
    BS      b1      branch if sign bit
    BZ      b2      branch if zero bit
    BC      b3      branch if carry bit
BL       c      branch with link
    BLS     c1      branch with link if sign bit
    BLZ     c2      branch with link if zero bit
    BLC     c3      branch with link if carry bit
PUSH    d       push to stack
POP     e       pop from stack
RET     f       return to linked address
```

### HLT ###

Halts execution of the computer.

### ADD, SUB, AND, OR, XOR, LSL, LSR ###

Mathmatical/ bitwise operations.

### MOV ###

Move a value into a target register. Value can be indirect, immediate byte, or immediate word value

### LDR, STR ###

Load/ store a value from/ to the bus.

### B, BS, BZ, BC ###

Branch opertations. If condition is met PC will be set to the given value.

### BL, BLS, BLZ, BLC ###

Branch with link opertations. If condition is met PC will be pushed to the stack and then PC will be set to the given value.

### PUSH, POP ###

Push/ pop a value to the stack.

### RET ###

Pop a linked address from the stack into PC.

## Opcode Formats ##

Note: Jump opcodes do not use a target register and its value is ignored

### Indirect ###

```
|   15  14  13  12  11  10  9   8   7   6   5   4   3   2   1   0
+------------------------------------------------------------------
|   [   opcode   ]  0   -   -   -   -   [ reg T ]   -   [ reg S ]
```

### Immediate byte ###

Only registers A-D can be target registers for immediate byte values. Immediate bytes are treated as signed PC relative values for jumps.

```
|   15  14  13  12  11  10  9   8   7   6   5   4   3   2   1   0
+------------------------------------------------------------------
|   [   opcode   ]  1   0   [reg T] [       immediate value       ]
```

### Immediate word ###

```
|   15  14  13  12  11  10  9   8   7   6   5   4   3   2   1   0   |   15  14  13  12  11  10  9   8   7   6   5   4   3   2   1   0  
+-------------------------------------------------------------------+------------------------------------------------------------------
|   [   opcode   ]  1   1   -   -   -   -   -   -   -   [ reg T ]   |   [                      immediate value                        ]
```

### Implict ###

#### HLT, RET ####

```
|   15  14  13  12  11  10  9   8   7   6   5   4   3   2   1   0
+--------------------------------------------------------------------
|   [   opcode   ]  -   -   -   -   -   -   -   -   -   -   -   -
```

#### PUSH, POP ####

```
|   15  14  13  12  11  10  9   8   7   6   5   4   3   2   1   0
+--------------------------------------------------------------------
|   [   opcode   ]  -   -   -   -   -   PC  SP  E   D   C   B   A
```

## BIOS ##

```
00      Soft reset
02      Interrupt vector
04      Sine
06      Cosine
08      ArcTan
0A      ArcTan2
```

## Picture Processing ##

### Mode 0 - 8bpp Indexed 256x128 ###

### Mode 1 - 16bpp Bitmap 128x128 ###

### Mode 2 - 2bpp Tile/ Sprite mode 256x128 ###

8x8 tiles
8 words/tile

256 tiles/tilemap
2048 words/tilemap

4 words/palette
8 palettes

```
0xC000-0xC7FF   Tile data
0xC800-0xCFFF   BG0 attributes  (2*2 screens)
0xD000-0xD7FF   BG1 attributes  (2*2 screens)
0xD800-0xEFFF   BG2 attributes  (2*2 screens)
0xF000-0xF07F   OAM data        (up to 64 sprites)
0xF080-0xF08F   Palette data    (4*4 16bpp direct colour values (mode 2 only))
0xF090-0xFFFD   Unused
0xFFF9          BG enable       (0=none, 1=BG0, 2=BG1, 4=BG2)
0xFFFA          BG0 X scroll
0xFFFB          BG0 Y scroll
0xFFFC          BG1 X scroll
0xFFFD          BG1 Y scroll
0xFFFE          BG2 X scroll
0xFFFF          BG2 Y scroll
```

#### Tile Attributes ####

```
|   15  14  13  12  11  10  9   8   7   6   5   4   3   2   1   0
+--------------------------------------------------------------------
|   fX  fY  -   -   -   [   pal   ] [          tile index          ]

fX  = flip X
fY  = flip Y
pal = palette index
```
1 word/tileAttr


32x16 tileAttr/screen
2x2 screens
=2048 words (0x800)



#### Sprite Attributes ####

```
|   15  14  13  12  11  10  9   8   7   6   5   4   3   2   1   0
+--------------------------------------------------------------------
|   c   [       y position       ]  [         x position         ]  

|   15  14  13  12  11  10  9   8   7   6   5   4   3   2   1   0
+--------------------------------------------------------------------
|   fX  fY  -   -   -   [   pal   ] [          tile index          ]


c   = check bit 1=sprite,0=stop
fX  = flip X
fY  = flip Y
pal = palette index
```