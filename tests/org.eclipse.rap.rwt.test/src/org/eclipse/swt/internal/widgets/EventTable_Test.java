/*******************************************************************************
 * Copyright (c) 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.swt.internal.widgets;

import static org.mockito.Mockito.mock;
import junit.framework.TestCase;

import org.eclipse.swt.internal.events.EventTable;
import org.eclipse.swt.widgets.Listener;


public class EventTable_Test extends TestCase {
  
  private static final int EVENT_1 = 1;
  private EventTable eventTable;

  @Override
  protected void setUp() throws Exception {
    eventTable = new EventTable();
  }
  
  public void testHook() {
    Listener listener = mock( Listener.class );
    
    eventTable.hook( EVENT_1, listener );
    
    assertEquals( 1, eventTable.size() );
  }
  
  public void testUnhook() {
    Listener listener = mock( Listener.class );
    eventTable.hook( EVENT_1, listener );
    
    eventTable.unhook( EVENT_1, listener );
    
    assertEquals( 0, eventTable.size() );
  }
  
  public void testUnhookUnknownEventType() {
    Listener listener = mock( Listener.class );
    eventTable.hook( EVENT_1, listener );
    
    eventTable.unhook( 23, listener );
    
    assertEquals( 1, eventTable.size() );
  }

  public void testUnhookUnknownListener() {
    Listener listener = mock( Listener.class );
    eventTable.hook( EVENT_1, listener );
    
    eventTable.unhook( EVENT_1, mock( Listener.class ) );
    
    assertEquals( 1, eventTable.size() );
  }
  
  public void testHooksUnknownEventType() {
    boolean hooks = eventTable.hooks( 23 );
    
    assertFalse( hooks );
  }

  public void testHooksKnownEventType() {
    eventTable.hook( EVENT_1, mock( Listener.class ) );

    boolean hooks = eventTable.hooks( EVENT_1 );
    
    assertTrue( hooks );
  }
}
