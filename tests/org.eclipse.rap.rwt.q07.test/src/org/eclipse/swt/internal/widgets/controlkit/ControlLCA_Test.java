/*******************************************************************************
 * Copyright (c) 2002, 2008 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 ******************************************************************************/
package org.eclipse.swt.internal.widgets.controlkit;

import java.io.IOException;

import junit.framework.TestCase;

import org.eclipse.rwt.Fixture;
import org.eclipse.rwt.graphics.Graphics;
import org.eclipse.rwt.internal.browser.Mozilla1_7up;
import org.eclipse.rwt.internal.lifecycle.DisplayUtil;
import org.eclipse.rwt.internal.service.RequestParams;
import org.eclipse.rwt.internal.theme.ThemeManager;
import org.eclipse.rwt.lifecycle.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.events.ActivateAdapter;
import org.eclipse.swt.internal.events.ActivateEvent;
import org.eclipse.swt.internal.widgets.Props;
import org.eclipse.swt.widgets.*;


public class ControlLCA_Test extends TestCase {

  protected void setUp() throws Exception {
    Fixture.setUpWithoutResourceManager();
    ThemeManager.getInstance().initialize();
  }

  protected void tearDown() throws Exception {
// TODO [rst] Keeping the ThemeManager initialized speeds up TestSuite
//    ThemeManager.getInstance().deregisterAll();
    Fixture.tearDown();
  }

  public void testPreserveValues() {
    Display display = new Display();
    Composite shell = new Shell( display , SWT.NONE );
    Button button = new Button( shell, SWT.PUSH );
    Boolean hasListeners;
    Fixture.markInitialized( display );
    //bound
    Rectangle rectangle = new Rectangle( 10, 10, 10, 10 );
    button.setBounds( rectangle );
    Fixture.preserveWidgets();
    IWidgetAdapter adapter = WidgetUtil.getAdapter( button );
    assertEquals( rectangle, adapter.getPreserved( Props.BOUNDS ) );
    Fixture.clearPreserved();
    //z-index
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    assertTrue( adapter.getPreserved( Props.Z_INDEX ) != null );
    Fixture.clearPreserved();
    //visible
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    assertEquals( Boolean.TRUE, adapter.getPreserved( Props.VISIBLE ) );
    Fixture.clearPreserved();
    button.setVisible( false );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    assertEquals( Boolean.FALSE, adapter.getPreserved( Props.VISIBLE ) );
    Fixture.clearPreserved();
    //enabled
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    assertEquals( Boolean.TRUE, adapter.getPreserved( Props.ENABLED ) );
    Fixture.clearPreserved();
    button.setEnabled( false );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    assertEquals( Boolean.FALSE, adapter.getPreserved( Props.ENABLED ));
    Fixture.clearPreserved();
    //control_listeners
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    hasListeners = ( Boolean )adapter.getPreserved( Props.CONTROL_LISTENERS );
    assertEquals( Boolean.FALSE, hasListeners );
    Fixture.clearPreserved();
    button.addControlListener( new ControlAdapter() { } );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    hasListeners = ( Boolean ) adapter.getPreserved( Props.CONTROL_LISTENERS );
    assertEquals( Boolean.TRUE, hasListeners );
    Fixture.clearPreserved();
    //foreground background font
    Color background = Graphics.getColor( 122, 33, 203 );
    button.setBackground( background );
    Color foreground = Graphics.getColor( 211, 178, 211 );
    button.setForeground( foreground );
    Font font = Graphics.getFont( "font", 12, SWT.BOLD );
    button.setFont( font );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    assertEquals( background, adapter.getPreserved( Props.BACKGROUND ) );
    assertEquals( foreground, adapter.getPreserved( Props.FOREGROUND ) );
    assertEquals( font, adapter.getPreserved( Props.FONT ) );
    Fixture.clearPreserved();
    //tab_index
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    assertTrue( adapter.getPreserved( Props.Z_INDEX ) != null );
    Fixture.clearPreserved();
    //tooltiptext
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    assertEquals( null, button.getToolTipText() );
    Fixture.clearPreserved();
    button.setToolTipText( "some text" );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    assertEquals( "some text", button.getToolTipText() );
    Fixture.clearPreserved();
    //activate_listeners   Focus_listeners
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    hasListeners = ( Boolean )adapter.getPreserved( Props.FOCUS_LISTENER );
    assertEquals( Boolean.FALSE, hasListeners );
    Fixture.clearPreserved();
    button.addFocusListener( new FocusAdapter() { } );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    hasListeners = ( Boolean ) adapter.getPreserved( Props.FOCUS_LISTENER );
    assertEquals( Boolean.TRUE, hasListeners );
    Fixture.clearPreserved();
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    hasListeners = ( Boolean )adapter.getPreserved( Props.ACTIVATE_LISTENER );
    assertEquals( Boolean.FALSE, hasListeners );
    Fixture.clearPreserved();
    ActivateEvent.addListener( button, new ActivateAdapter() { } );
    Fixture.preserveWidgets();
    adapter = WidgetUtil.getAdapter( button );
    hasListeners = ( Boolean ) adapter.getPreserved( Props.ACTIVATE_LISTENER );
    assertEquals( Boolean.TRUE, hasListeners );
    Fixture.clearPreserved();
    display.dispose();
  }

  public void testWriteVisibility() throws IOException {
    Display display = new Display();
    Shell shell = new Shell( display , SWT.NONE );
    Button button = new Button( shell, SWT.PUSH );
    shell.open();
    AbstractWidgetLCA lca = WidgetUtil.getLCA( button );

    // Initial JavaScript code must not contain setVisibility()
    Fixture.fakeResponseWriter();
    lca.renderInitialization( button );
    lca.renderChanges( button );
    assertTrue( Fixture.getAllMarkup().indexOf( "setVisibility" ) == -1 );

    // Unchanged visible attribute must not be rendered
    Fixture.fakeResponseWriter();
    Fixture.markInitialized( display );
    Fixture.markInitialized( button );
    Fixture.preserveWidgets();
    lca.renderInitialization( button );
    lca.renderChanges( button );
    assertTrue( Fixture.getAllMarkup().indexOf( "setVisibility" ) == -1 );

    // Changed visible attribute must not be rendered
    Fixture.fakeResponseWriter();
    Fixture.preserveWidgets();
    button.setVisible( false );
    lca.renderInitialization( button );
    lca.renderChanges( button );
    assertTrue( Fixture.getAllMarkup().indexOf( "setVisibility" ) != -1 );
  }

  public void testWriteBounds() throws IOException {
    Fixture.fakeBrowser( new Mozilla1_7up( true, true ) );
    Display display = new Display();
    Shell shell = new Shell( display , SWT.NONE );
    Control control = new Button( shell, SWT.PUSH );
    Composite parent = control.getParent();

    // call writeBounds once to elimniate the uninteresting JavaScript prolog
    Fixture.fakeResponseWriter();
    WidgetLCAUtil.writeBounds( control, parent, control.getBounds() );

    // Test without clip
    Fixture.fakeResponseWriter();
    control.setBounds( 1, 2, 100, 200 );
    WidgetLCAUtil.writeBounds( control, parent, control.getBounds() );
    String expected = "w.setSpace( 1, 100, 2, 200 );";
    assertEquals( expected, Fixture.getAllMarkup() );
  }

  public void testWriteFocusListener() throws IOException {
    FocusAdapter focusListener = new FocusAdapter() {
    };
    Display display = new Display();
    Shell shell = new Shell( display );
    Label label = new Label( shell, SWT.NONE );
    label.addFocusListener( focusListener );
    Button button = new Button( shell, SWT.PUSH );
    button.addFocusListener( focusListener );
    // Test that JavaScript focus listeners are rendered for a focusable control
    // (e.g. button)
    Fixture.fakeResponseWriter();
    ControlLCAUtil.writeChanges( button ); // calls writeFocusListener
    String focusGained = "org.eclipse.swt.EventUtil.focusGained";
    String focusLost = "org.eclipse.swt.EventUtil.focusLost";
    String markup = Fixture.getAllMarkup();
    assertTrue( markup.indexOf( focusGained ) != -1 );
    assertTrue( markup.indexOf( focusLost ) != -1 );

    // Test that for a non-focusable control (e.g. label), no focus-listener
    // JavaScript code is emitted
    Fixture.fakeResponseWriter();
    ControlLCAUtil.writeChanges( label );
    markup = Fixture.getAllMarkup();
    assertEquals( -1, markup.indexOf( focusGained ) );
    assertEquals( -1, markup.indexOf( focusLost ) );
  }

  public void testRedrawAndDispose() {
    final StringBuffer log = new StringBuffer();
    // Set up test scenario
    Display display = new Display();
    Shell shell = new Shell( display );
    Control control = new Composite( shell, SWT.NONE ) {
      public Object getAdapter( final Class adapter ) {
        Object result;
        if( adapter == ILifeCycleAdapter.class ) {
          result = new AbstractWidgetLCA() {
            public void preserveValues( final Widget widget ) {
            }
            public void renderChanges( final Widget widget )
              throws IOException
            {
            }
            public void renderDispose( final Widget widget )
              throws IOException
            {
              log.append( "renderDispose" );
            }
            public void renderInitialization( final Widget widget )
              throws IOException
            {
            }
            public void readData( final Widget widget ) {
            }
            public void doRedrawFake( final Control control ) {
              log.append( "FAILED: doRedrawFake was called" );
            }
          };
        } else {
          result = super.getAdapter( adapter );
        }
        return result;
      }
    };
    Fixture.markInitialized( display );
    Fixture.markInitialized( shell );
    Fixture.markInitialized( control );
    // redraw & dispose: must revoke redraw
    control.redraw();
    control.dispose();
    // run life cycle that (in this case) won't call doRedrawFake
    Fixture.fakeResponseWriter();
    String displayId = DisplayUtil.getId( display );
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( "renderDispose", log.toString() );
  }
}
