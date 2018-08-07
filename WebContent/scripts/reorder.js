/*

Copyright (C) 2014 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. The terms require you to include
the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

var _reorderDetails = null;

function addReorder(collection, items, rerender) {
	
	// loop all the drag images
	items.each( function(index) {
		
		// get reference to image
		var img = $(this);
		
		// disable standard drag
		addListener( img.on('dragstart', function(ev) { 
			event.preventDefault(); 
		}));
		
		// add mousedown
		addListener( img.mousedown( {collection: collection}, function(ev){
			// get a reference to the fromItem (previously an image, but now a div)
			var fromItem = $(this);
			// retain a reference to the fromIndex
			_reorderDetails = { fromIndex: items.index(fromItem), collection: ev.data.collection };
		}));
		
		// add mousemove 
		addListener( img.mouseenter( {collection: collection, rerender: rerender}, function(ev){
			
			// get a reference to the collection object of the image we've just hit
			var collection = ev.data.collection;
			
			// if there are reorder details and we have a fromIndex
			if (_reorderDetails && _reorderDetails.fromIndex >= 0) {
				
				// only if the object are from the same collection
				if (_reorderDetails.collection === collection) {
				
					// assume we can't find the object we are moving from
					var fromIndex = 	_reorderDetails.fromIndex;
					// get a reference to the potential reorder to image
					var reorderTo = $(this);
					// retain the position we are moving to
					var toIndex = items.index(reorderTo);

					// if the from and to are found and different
					if (toIndex >= 0 && fromIndex >= 0 && toIndex != fromIndex) {
						// retain the object we are moving as the "from"
						var fromObject = collection[fromIndex];
						// check whether we're replacing up or down
						if (fromIndex > toIndex) {
							// a lower object has been moved up - shift all objects above from down one
							for (var i = fromIndex; i > toIndex ; i--) {
								collection[i] = collection[i - 1];
							}
							// put the from into the to
							collection[toIndex] = fromObject;
						} else {
							// a high object has been moved down - shift all objects below to up one
							for (var i = fromIndex; i < toIndex; i++) {
								collection[i] = collection[i + 1];
							}
							// put the from into the to
							collection[toIndex] = fromObject;
						}
						// make the to object null as we will loose it in the rerender, but set the fromIndex to the toIndex so we can move again from where are now
						_reorderDetails = { fromIndex: toIndex, collection: collection };
						// re-render 
						ev.data.rerender();
						
					}
				}
			}
		}));
		
	});
}
