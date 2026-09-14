---
navigation:
    title: Modifier
    icon: 'castingmb:mb_controller'
    parent: index.md
    position: 200
item_ids:
    - 'castingmb:mb_controller'
    - 'castingmb:mb_solidifier'
    - 'castingmb:mb_tank'
    - 'castingmb:mb_black_bricks'
    - 'castingmb:mb_regulator'
    - 'castingmb:mb_black_brick_glass'
    - 'castingmb:mb_interface'

---

# Multiblock

## Multiblock Controller 

The Multiblock Controller is the heart of Casting Multiblocks. This block functionally works the same as the normal controller but with a couple of changes. Depending on the size of the multiblock the controller can have up to 100 slots for melting items and tank scales up depending on the size as well. For each internal air block inside the multiblock controller you gain an additional item slot and 1000mb of tank capacity. The tank is also a single shared tank.

The max size of the multiblock is 1024 volume of air. Up to 128 across and 64 blocks high. If the size exceeds 1024 volume the controller will not work. Corners are also optional in the multiblock 

## Multiblock Solidifier
The Multiblock Solidifier is a solidifier that can be used in the multiblock, it works the same as the normal solidifier but uses the shared tank of the multiblock controller and can be placed anywhere in the multiblock structure. You can use as many of these as you want. Can be sped up with cooling fluids inside Multiblock Tanks

## Multiblock Tank
The Multiblock Tank is a tank that can be used in the multiblock. You can place as many of these as you want but the controller will always use the hottest fuel tank and Solidifiers will always use the coldest fuel tanks. Can be placed anywhere in the multiblock structure.

## Multiblock Bricks and Glass
Used to fill create the walls and floor of the structure, Glass can be used to view inside the structure

## Multiblock Regulators
The Multiblock Regulator is used to regulate the amount of fluid types that the multiblock tank can add. Without regulators the multiblock tank can add any amount of fluid types. For each regulator added will allow you to store an addition fluid type. So with 3 regulators you can store 3 different fluid types in the multiblock tank. The amount of regulators will show when hovering over Multiblock Controller Tank.

## Multiblock Interface
The Multiblock Interface is a block that can be placed anywhere in the multiblock structure and can be used to insert and extract fluids. It will also ad items to the controller to melt and extract resulting items from the Solidifiers inside the multiblock

## Entity Melting
Some entities can be melted inside the multiblock into fluids seems completely fine right

## Examples 1
A simple 3x3x3 multiblock with 27 internal air blocks, so it has 27 item slots and a tank capacity of 27000mb. It has no regulators so it can store any amount of fluid types in the tank.
<GameScene zoom="3" interactive={true}>
  <ImportStructure src="assets/structures/simple.nbt" />
</GameScene>

## Example 2
This multiblock has 3 regulators so it can only store up to 3 different fluid types in the tank.
<GameScene zoom="3" interactive={true}>
  <ImportStructure src="assets/structures/simple_regulators.nbt" />
</GameScene>