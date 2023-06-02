# GUIMarketplaceDirectory

#### An easy to use GUI based marketplace directory

Do you ever play in a large co-op SMP and spend hours searching for the perfect deals on an item you need in the marketplace? Do you have to spend ages finding what you actually need? GUIMD provides a 
simple easy to use solution to find what you want without the need to traverse around the marketplace.

### How to use
#### Viewing the Marketplace
The shop data is stored in json format in the plugin's directory, making it exportable to other servers (and conversely importable). To make yourself a copy of the Marketplace Directory, sign a book 
with the title `[Marketplace]`. Alternatively, you can use the `/guimd dir` command to get yourself a copy of the directory. <br>
This signed book will now serve as a gateway to the marketplace. Interact with the book as you would do with a regular signed book, and you will be able to see all
shops registered in the directory. You can view the items sold in each shop by clicking on the respective shop names in the inventory 
style GUI now open on your screen.

<img src="Captures/init_marketplace.gif">

The items will have their order quantity and value displayed with them. Right click on any item to find better alternatives if they exist.

#### Making your own shop
##### Getting your shop listed
Write the name and description of your shop in a book in the format
````
[<shop-name>]
[<description>]
[<display-item>](optional)
````

Optional: The default display for shops in the directory is a written book. You can change that by including <br>
the optional `[<display-item>]` tag in the book. Item name has to match material names in minecraft. Eg:  <br>
Beacon: `beacon or BEACON` <br>
Netherite Chestplate: `netherite chestplate or netherite_chestplate or NETHERITE_CHESTPLATE` <br>
Or you can choose your custom item later. <br>

Make sure to not use any more "\[" and "\]" than required. <br>
Sign it with the title `[shop init]` or `[init shop]`. 

```
NOTE: Where you sign your book matters!
The coordinates of where the book was signed will be saved as your shop's coordinates.
```
<img src="Captures/init_shop.gif"> <br> <br>
<img src="Captures/view_marketplace.gif">

##### Adding Items
Open your inventory and right click on an item you want to add with the book as shown:

<img src="Captures/add_item.gif">

You will be prompted to first enter the unit amount in the form `<shulker>:<stack>:<unit>`, and the the price in diamonds.

In case you want to add different versions of the same item, do this:

<img src="Captures/item_modify.gif">

Make sure to set quantity as per the rule stated, or you item addition will be aborted. Also, use common sense to add quantities, for eg, if you want to sell a shulker of coal, you could either 
sell coal with the unit amount in the format `1:0:0`, or you could sell a shulker renamed to "Shulker of Coal" with unit amount in the format `0:0:1`.

#### Searching for items
You can search the directory by either items sold, shop name, or owner's name. The `/guimd search` command has been provided. Usage:

`/guimd search item <search-key>`: Returns all items that contain the search key, be it regular items or renamed ones.

`/guimd search shop <search-key>`: Returns all shops that match the search key.

`/guimd search player <search-key>`: Returns all shops whose owner's ign contains the search key.

<img src="Captures/search.gif">

#### Removing your shop
You can remove your shop by interacting with the shop book:

<img src="Captures/shop_edit.gif">

#### Setting the display item
You can add or modify your shop's custom display item with the shop book:

<img src="Captures/set_display_item.gif">

### Optional Utilities
#### Multi owner support
If enabled, it will allow multiple players to be owners of a shop (as is the case in large SMP's), and allow each of them to add items to the shop. As a side-effect, this will allow players to 
create shops for other players and assign the other player as the primary owner.

The current owner(s) will have to interact with their signed book as they would with a regular signed book, and they will be prompted to add the name of the owner they want to add.

#### Marketplace Directory Moderation
Allowing any player to create shops is a messy ordeal, and there can be rogue entries. If enabled, all new shops will enter a pending perms queue, waiting for the server appointed moderators to 
approve their shop listing. These shop owners can still add items to their shop. Such "pending" shopps won't be displayed in the regular Marketplace Directory. Appointed moderators will have to be 
given the `GUIMD.moderate` permission node. This will enable access to the `/guimd moderate` command. Usage:

`/guimd moderate pending`: This will open up an inventory style GUI similar to the Marketplace Directory, but will only contain shops that need approval. Moderators can right click on a shop to 
approve, or left click to reject. Approval is immediate, but rejection will provide an additional prompt to confirm the action.

`/guimd moderate review`: This will open up an inventory style GUI similar to the Marketplace Directory. Moderators can right click on a shop to remove a shop if they deem it so. This will prompt
the moderator to confirm the removal of the shop.

`/guimd moderate recover`: This utility can be used to recover shop edit books if the owner has lost it. This will open up an inventory style GUI similar to the Marketplace Directory containing 
approved shops. Moderators can right click on a shop to make a copy of the shop edit book. 

##### By default, these three utilities are enabled. It is recommended not to turn off moderation.