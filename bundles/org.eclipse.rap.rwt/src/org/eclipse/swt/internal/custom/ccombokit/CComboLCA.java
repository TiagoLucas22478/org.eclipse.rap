/*******************************************************************************
 * Copyright (c) 2009, 2011 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.swt.internal.custom.ccombokit;

import java.io.IOException;

import org.eclipse.rwt.internal.protocol.ClientObjectFactory;
import org.eclipse.rwt.internal.protocol.IClientObject;
import org.eclipse.rwt.internal.util.EncodingUtil;
import org.eclipse.rwt.internal.util.NumberFormatUtil;
import org.eclipse.rwt.lifecycle.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.widgets.Props;
import org.eclipse.swt.widgets.Widget;

public final class CComboLCA extends AbstractWidgetLCA {

  private static final String TYPE = "rwt.widgets.Combo";
  private static final String[] DEFAUT_ITEMS = new String[ 0 ];
  private static final Integer DEFAULT_SELECTION = new Integer( -1 );
  private static final Integer DEFAULT_TEXT_LIMIT = new Integer( CCombo.LIMIT );
  private static final Point DEFAULT_TEXT_SELECTION = new Point( 0, 0 );
  private static final Integer DEFAULT_VISIBLE_ITEM_COUNT = new Integer( 5 );

  // Constants for JS functions names
  private static final String JS_FUNC_SELECT = "select";
  private static final String JS_FUNC_SET_SELECTION_TEXT = "setTextSelection";

  // Property names for preserve-value facility
  static final String PROP_ITEMS = "items";
  static final String PROP_TEXT = "text";
  static final String PROP_SELECTION = "selection";
  static final String PROP_TEXT_SELECTION = "textSelection";
  static final String PROP_TEXT_LIMIT = "textLimit";
  static final String PROP_LIST_VISIBLE = "listVisible";
  static final String PROP_EDITABLE = "editable";
  static final String PROP_VERIFY_MODIFY_LISTENER = "verifyModifyListener";
  static final String PROP_VISIBLE_ITEM_COUNT = "visibleItemCount";
  static final String PROP_ITEM_HEIGHT = "itemHeight";

  public void preserveValues( Widget widget ) {
    CCombo ccombo = ( CCombo )widget;
    ControlLCAUtil.preserveValues( ccombo );
    IWidgetAdapter adapter = WidgetUtil.getAdapter( widget );
    String[] items = ccombo.getItems();
    adapter.preserve( PROP_ITEMS, items );
    Integer selection = new Integer( ccombo.getSelectionIndex() );
    adapter.preserve( PROP_SELECTION, selection );
    adapter.preserve( PROP_TEXT_SELECTION, ccombo.getSelection() );
    adapter.preserve( PROP_TEXT_LIMIT, new Integer( ccombo.getTextLimit() ) );
    adapter.preserve( PROP_VISIBLE_ITEM_COUNT, new Integer( ccombo.getVisibleItemCount() ) );
    adapter.preserve( PROP_ITEM_HEIGHT, new Integer( ccombo.getItemHeight() ) );
    adapter.preserve( PROP_TEXT, ccombo.getText() );
    adapter.preserve( Props.SELECTION_LISTENERS,
                      Boolean.valueOf( SelectionEvent.hasListener( ccombo ) ) );
    adapter.preserve( PROP_LIST_VISIBLE, new Boolean( ccombo.getListVisible() ) );
    adapter.preserve( PROP_EDITABLE, Boolean.valueOf( isEditable( ccombo ) ) );
    boolean hasVerifyListener = VerifyEvent.hasListener( ccombo );
    boolean hasModifyListener = ModifyEvent.hasListener( ccombo );
    boolean hasListener = hasVerifyListener || hasModifyListener;
    adapter.preserve( PROP_VERIFY_MODIFY_LISTENER, Boolean.valueOf( hasListener ) );
    WidgetLCAUtil.preserveCustomVariant( ccombo );
  }

  public void readData( Widget widget ) {
    final CCombo ccombo = ( CCombo )widget;
    String value = WidgetLCAUtil.readPropertyValue( ccombo, "selectedItem" );
    if( value != null ) {
      ccombo.select( NumberFormatUtil.parseInt( value ) );
    }
    String listVisible = WidgetLCAUtil.readPropertyValue( ccombo, "listVisible" );
    if( listVisible != null ) {
      ccombo.setListVisible( Boolean.valueOf( listVisible ).booleanValue() );
    }
    readTextAndSelection( ccombo );
    ControlLCAUtil.processSelection( ccombo, null, true );
    ControlLCAUtil.processMouseEvents( ccombo );
    ControlLCAUtil.processKeyEvents( ccombo );
    ControlLCAUtil.processMenuDetect( ccombo );
    WidgetLCAUtil.processHelp( ccombo );
  }

  public void renderInitialization( Widget widget ) throws IOException {
    CCombo ccombo = ( CCombo )widget;
    IClientObject clientObject = ClientObjectFactory.getForWidget( ccombo );
    clientObject.create( TYPE );
    clientObject.setProperty( "parent", WidgetUtil.getId( ccombo.getParent() ) );
    clientObject.setProperty( "style", WidgetLCAUtil.getStyles( ccombo ) );
    clientObject.setProperty( "ccombo", true );
  }

  public void renderChanges( Widget widget ) throws IOException {
    CCombo ccombo = ( CCombo )widget;
    ControlLCAUtil.renderChanges( ccombo );
    WidgetLCAUtil.renderCustomVariant( ccombo );
    renderItemHeight( ccombo );
    renderVisibleItemCount( ccombo );
    writeListVisible( ccombo );
    writeItems( ccombo );
    writeSelection( ccombo );
    writeEditable( ccombo );
    writeText( ccombo );
    writeTextSelection( ccombo );
    writeTextLimit( ccombo );
    writeVerifyAndModifyListener( ccombo );
    writeSelectionListener( ccombo );
  }

  public void renderDispose( Widget widget ) throws IOException {
    ClientObjectFactory.getForWidget( widget ).destroy();
  }

  ///////////////////////////////////////
  // Helping methods to read client state

  private static void readTextAndSelection( final CCombo ccombo ) {
    final Point selection = readSelection( ccombo );
    final String txt = WidgetLCAUtil.readPropertyValue( ccombo, "text" );
    if( txt != null ) {
      if( VerifyEvent.hasListener( ccombo ) ) {
        // setText needs to be executed in a ProcessAcction runnable as it may
        // fire a VerifyEvent whose fields (text and doit) need to be evaluated
        // before actually setting the new value
        ProcessActionRunner.add( new Runnable() {
          public void run() {
            ccombo.setText( txt );
            // since text is set in process action, preserved values have to be
            // replaced
            IWidgetAdapter adapter = WidgetUtil.getAdapter( ccombo );
            adapter.preserve( PROP_TEXT, txt );
            if( selection != null ) {
              ccombo.setSelection( selection );
              adapter.preserve( PROP_TEXT_SELECTION, selection );
            }
         }
        } );
      } else {
        ccombo.setText( txt );
        if( selection != null ) {
          ccombo.setSelection( selection );
        }
      }
    } else if( selection != null ) {
      ccombo.setSelection( selection );
    }
  }

  private static Point readSelection( CCombo ccombo ) {
    Point result = null;
    String selStart = WidgetLCAUtil.readPropertyValue( ccombo, "selectionStart" );
    String selLength = WidgetLCAUtil.readPropertyValue( ccombo, "selectionLength" );
    if( selStart != null || selLength != null ) {
      result = new Point( 0, 0 );
      if( selStart != null ) {
        result.x = NumberFormatUtil.parseInt( selStart );
      }
      if( selLength != null ) {
        result.y = result.x + NumberFormatUtil.parseInt( selLength );
      }
    }
    return result;
  }

  //////////////////////////////////////////////
  // Helping methods to write changed properties

  private static void renderItemHeight( CCombo ccombo ) {
    Integer newValue = new Integer( ccombo.getItemHeight() );
    if( WidgetLCAUtil.hasChanged( ccombo, PROP_ITEM_HEIGHT, newValue ) ) {
      IClientObject clientObject = ClientObjectFactory.getForWidget( ccombo );
      clientObject.setProperty( PROP_ITEM_HEIGHT, newValue );
    }
  }

  private static void renderVisibleItemCount( CCombo ccombo ) {
    Integer newValue = new Integer( ccombo.getVisibleItemCount() );
    Integer defValue = DEFAULT_VISIBLE_ITEM_COUNT;
    if( WidgetLCAUtil.hasChanged( ccombo, PROP_VISIBLE_ITEM_COUNT, newValue, defValue ) ) {
      IClientObject clientObject = ClientObjectFactory.getForWidget( ccombo );
      clientObject.setProperty( PROP_VISIBLE_ITEM_COUNT, newValue );
    }
  }

  private static void writeItems( CCombo ccombo ) throws IOException {
    JSWriter writer = JSWriter.getWriterFor( ccombo );
    String[] items = ccombo.getItems();
    if( WidgetLCAUtil.hasChanged( ccombo, PROP_ITEMS, items, DEFAUT_ITEMS ) ) {
      // Convert newlines into whitespaces
      for( int i = 0; i < items.length; i++ ) {
        items[ i ] = WidgetLCAUtil.replaceNewLines( items[ i ], " " );
        items[ i ] = WidgetLCAUtil.escapeText( items[ i ], false );
        items[ i ] = EncodingUtil.replaceWhiteSpaces( items[ i ] );
      }
      writer.set( PROP_ITEMS, new Object[] { items } );
    }
  }

  private static void writeSelection( CCombo ccombo ) throws IOException {
    Integer newValue = new Integer( ccombo.getSelectionIndex() );
    Integer defValue = DEFAULT_SELECTION;
    boolean selectionChanged
      = WidgetLCAUtil.hasChanged( ccombo, PROP_SELECTION, newValue, defValue );
    // The 'textChanged' statement covers the following use case:
    // ccombo.add( "a" );  ccombo.select( 0 );
    // -- in a subsequent request --
    // ccombo.removeAll();  ccombo.add( "b" );  ccombo.select( 0 );
    // When only examining selectionIndex, a change cannot be determined
    boolean textChanged = !isEditable( ccombo )
                          && WidgetLCAUtil.hasChanged( ccombo, PROP_TEXT, ccombo.getText(), "" );
    if( selectionChanged || textChanged ) {
      JSWriter writer = JSWriter.getWriterFor( ccombo );
      writer.call( JS_FUNC_SELECT, new Object[] { newValue } );
    }
  }

  private static void writeTextSelection( CCombo ccombo ) throws IOException {
    Point newValue = ccombo.getSelection();
    Point defValue = DEFAULT_TEXT_SELECTION;
    Integer start = new Integer( newValue.x );
    Integer end = new Integer( newValue.y );
    Integer count = new Integer( end.intValue() - start.intValue() );
    // TODO [rh] could be optimized: when text was changed and selection is 0,0
    //      there is no need to write JavaScript since the client resets the
    //      selection as well when the new text is set.
    if( WidgetLCAUtil.hasChanged( ccombo, PROP_TEXT_SELECTION, newValue, defValue ) ) {
      // [rh] Workaround for bug 252462: Changing selection on a hidden text
      // widget causes exception in FF
      if( ccombo.isVisible() ) {
        JSWriter writer = JSWriter.getWriterFor( ccombo );
        writer.call( JS_FUNC_SET_SELECTION_TEXT, new Object[] { start, count } );
      }
    }
  }

  private static void writeTextLimit( CCombo ccombo ) throws IOException {
    JSWriter writer = JSWriter.getWriterFor( ccombo );
    Integer newValue = new Integer( ccombo.getTextLimit() );
    Integer defValue = DEFAULT_TEXT_LIMIT;
    if( WidgetLCAUtil.hasChanged( ccombo, PROP_TEXT_LIMIT, newValue, defValue ) ) {
      if( newValue.intValue() == CCombo.LIMIT ) {
        newValue = null;
      }
      writer.set( "textLimit", newValue );
    }
  }

  private static void writeListVisible( CCombo ccombo ) throws IOException {
    boolean listVisible = ccombo.getListVisible();
    Boolean newValue = Boolean.valueOf( listVisible );
    if( WidgetLCAUtil.hasChanged( ccombo, PROP_LIST_VISIBLE, newValue ) ) {
      JSWriter writer = JSWriter.getWriterFor( ccombo );
      writer.set( PROP_LIST_VISIBLE, "listVisible", newValue, null );
    }
  }

  private static void writeEditable( CCombo ccombo ) throws IOException {
    boolean editable = isEditable( ccombo );
    Boolean newValue = Boolean.valueOf( editable );
    if( WidgetLCAUtil.hasChanged( ccombo, PROP_EDITABLE, newValue ) ) {
      JSWriter writer = JSWriter.getWriterFor( ccombo );
      writer.set( PROP_EDITABLE, "editable", newValue, null );
    }
  }

  private static void writeText( CCombo ccombo ) throws IOException {
    if( isEditable( ccombo ) || ccombo.getSelectionIndex() == -1 ) {
      String newValue = ccombo.getText();
      JSWriter writer = JSWriter.getWriterFor( ccombo );
      if( WidgetLCAUtil.hasChanged( ccombo, PROP_TEXT, newValue, "" ) ) {
        String value = EncodingUtil.removeNonDisplayableChars( newValue );
        writer.set( "value", value );
      }
    }
  }

  private static void writeSelectionListener( CCombo ccombo ) throws IOException {
    boolean hasListener = SelectionEvent.hasListener( ccombo );
    Boolean newValue = Boolean.valueOf( hasListener );
    String prop = Props.SELECTION_LISTENERS;
    if( WidgetLCAUtil.hasChanged( ccombo, prop, newValue, Boolean.FALSE ) ) {
      JSWriter writer = JSWriter.getWriterFor( ccombo );
      writer.set( "hasSelectionListener", newValue );
    }
  }

  private static void writeVerifyAndModifyListener( CCombo ccombo ) throws IOException {
    boolean hasVerifyListener = VerifyEvent.hasListener( ccombo );
    boolean hasModifyListener = ModifyEvent.hasListener( ccombo );
    boolean hasListener = hasVerifyListener || hasModifyListener;
    Boolean newValue = Boolean.valueOf( hasListener );
    String prop = PROP_VERIFY_MODIFY_LISTENER;
    if( WidgetLCAUtil.hasChanged( ccombo, prop, newValue, Boolean.FALSE ) ) {
      JSWriter writer = JSWriter.getWriterFor( ccombo );
      writer.set( "hasVerifyModifyListener", newValue );
    }
  }

  //////////////////
  // Helping methods

  private static boolean isEditable( CCombo ccombo ) {
    return ( ( ccombo.getStyle() & SWT.READ_ONLY ) == 0 );
  }
}
