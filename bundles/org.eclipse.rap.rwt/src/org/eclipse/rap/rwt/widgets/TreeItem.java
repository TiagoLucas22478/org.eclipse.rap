/*******************************************************************************
 * Copyright (c) 2002-2006 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 ******************************************************************************/

package org.eclipse.rap.rwt.widgets;

import org.eclipse.rap.rwt.RWT;


/**
 * TODO: [fappel] comment
 */
public class TreeItem extends Item {

  private final TreeItem parentItem;
  private final Tree parent;
  private final ItemHolder itemHolder;
  private boolean expanded;

  public TreeItem( final Tree parent, final int style ) {
    this( parent, null, style );
  }

  public TreeItem( final TreeItem parentItem, final int style ) {
    this( parentItem == null
                            ? null
                            : parentItem.parent, parentItem, style );
  }

  private TreeItem( final Tree parent,
                    final TreeItem parentItem,
                    final int style )
  {
    super( parent, style );
    this.parent = parent;
    this.parentItem = parentItem;
    if( parentItem != null ) {
      ItemHolder.addItem( parentItem, this );
    } else {
      ItemHolder.addItem( parent, this );
    }
    itemHolder = new ItemHolder( TreeItem.class );
  }

  public Object getAdapter( final Class adapter ) {
    Object result;
    if( adapter == IItemHolderAdapter.class ) {
      result = itemHolder;
    } else {
      result = super.getAdapter( adapter );
    }
    return result;
  }

  /////////////////
  // Item overrides
  
  public final Display getDisplay() {
    return parent.getDisplay();
  }

  /////////////////////////
  // Parent/child relations
  
  public final Tree getParent() {
    return parent;
  }

  public TreeItem getParentItem() {
    return parentItem;
  }
  
  ////////////////
  // Getter/Setter

  public void setExpanded( final boolean expanded ) {
    if( !expanded || getItemCount() > 0 ) {
      this.expanded = expanded; 
    }
  }
  
  public boolean getExpanded() {
    return expanded;
  }

  ///////////////////////////////////////
  // Methods to maintain (sub-) TreeItems
  
  public TreeItem[] getItems() {
    return ( TreeItem[] )itemHolder.getItems();
  }
  
  public TreeItem getItem( final int index ) {
    return ( TreeItem )itemHolder.getItem( index );
  }
  
  public int getItemCount() {
    return itemHolder.size();
  }
  
  public int indexOf( final TreeItem item ) {
    if( item == null ) {
      RWT.error( RWT.ERROR_NULL_ARGUMENT );
    }
    if( item.isDisposed() ) {
      RWT.error( RWT.ERROR_INVALID_ARGUMENT );
    }
    return itemHolder.indexOf( item );
  }

  public void removeAll() {
    TreeItem[] items = getItems();
    for( int i = 0; i < items.length; i++ ) {
      items[ i ].dispose();
    }
  }

  ///////////////////////////////////
  // Methods to dispose of the widget
  
  protected final void releaseChildren() {
    TreeItem[] items = getItems();
    for( int i = 0; i < items.length; i++ ) {
      items[ i ].dispose();
    }
  }

  protected final void releaseParent() {
    if( parentItem != null ) {
      ItemHolder.removeItem( parentItem, this );
      if( parentItem.getItemCount() == 0 ) {
        parentItem.setExpanded( false );
      }
    } else {
      ItemHolder.removeItem( parent, this );
    }
  }

  protected final void releaseWidget() {
  }
}
