# Specify (/speckit.specify)

/speckit.specify Develop "Store it!", an organiser/locator platform for individuals' and families' belongings. The application should allow users to register storage racks and fill them with their things or items.

## User journey

"Store it!" requires the user to have at least one registered rack to start adding items. Registering a rack starts by taking a photo of the rack, or a picture from the device gallery. Then, the user may add some relevant information, such as a name/identifier, a description, a location, etc. The rack picture will be later used as a map so that the user can tap on it, normally on a shelf slot, and add the item to those specific coordinates. Obviously, more items can be added to the same shelf slot.

On a registered rack, a user may perform two main actions: Adding an item to a rack or locating an item in a rack.

### Adding an item to a rack

The process starts by taking a photo of an item, or a picture from the device gallery. Then, the user may add some relevant information, such as a name, a description, a quantity value in case it applies, an owner, tags to easily group items, etc. Finally, the user selects the rack in which to place the item, and the shelf slot from that chosen rack by tapping on the rack picture.

This action may be started in reverse order if the user first selects a rack, taps on its associated picture and proceeds to add an item to it (taking a picture of the item, adding some relevant information, etc.). However, the final result will be the same.

### Locating an item in a rack

The user gets access to a registered rack and taps on any existing shelf slot, revealing a list of existing items. From there, the user can tap on any of those items and consult or edit any associated data.

Furthermore, there should be a search box available at any time, so the user can type something and all available items get searched by their names and descriptions.

All data must be properly persisted, although this feature will be achieved progressively. There will be no persistency initially. In a new iteration, a local database will be implemented. Finally, remote persistency will kick in.

Ideally, "Store it!" will require users to log-in/sign-up to start working, so their data can be loaded on any device. However, this won't be included in the MVP version, but later.
