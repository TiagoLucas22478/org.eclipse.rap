/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.rap.rwt.internal.protocol;

import static java.util.Arrays.asList;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rap.json.JsonObject;
import org.eclipse.rap.rwt.internal.protocol.Operation.SetOperation;
import org.junit.Test;


public class MessageImpl_Test {

  @Test
  public void testDefaultConstructor() {
    Message message = new MessageImpl();

    assertEquals( new JsonObject(), message.getHead() );
    assertEquals( new ArrayList<Operation>(), message.getOperations() );
  }

  @Test
  public void testConstructor_Message() {
    JsonObject head = new JsonObject().add( "foo", 23 );
    Operation operation = new SetOperation( "target", new JsonObject().add( "bar", 42 ) );
    Message originalMessage = mock( Message.class );
    when( originalMessage.getHead() ).thenReturn( head );
    when( originalMessage.getOperations() ).thenReturn( asList( operation ) );

    MessageImpl message = new MessageImpl( originalMessage );

    assertEquals( head, message.getHead() );
    assertEquals( asList( operation ), message.getOperations() );
  }

  @Test( expected = NullPointerException.class )
  public void testConstructor_Message_withNull() {
    new MessageImpl( (Message)null );
  }

  @Test( expected = NullPointerException.class )
  public void testConstructor_JsonObject_withNull() {
    new MessageImpl( (JsonObject)null );
  }

  @Test
  public void testConstructor_JsonObject_withEmptyObject() {
    try {
      new MessageImpl( new JsonObject() );
      fail();
    } catch( IllegalArgumentException exception ) {
      assertTrue( exception.getMessage().startsWith( "Failed to read head from JSON message" ) );
    }
  }

  @Test
  public void testConstructor_JsonObject_withoutOperations() {
    try {
      new MessageImpl( new JsonObject().add( "head", new JsonObject() ) );
      fail();
    } catch( IllegalArgumentException exception ) {
      assertTrue( exception.getMessage().contains( "Failed to read operations from JSON message" ) );
    }
  }

  @Test
  public void testConstructor_JsonObject_withInvalidOperationsMember() {
    String json = "{ \"head\" : {}, \"operations\" : 23 }";

    try {
      new MessageImpl( JsonObject.readFrom( json ) );
      fail();
    } catch( IllegalArgumentException exception ) {
      assertTrue( exception.getMessage().contains( "Failed to read operations from JSON message" ) );
    }
  }

  @Test
  public void testConstructor_JsonObject_withIllegalOperationInArray() {
    String json = "{ \"head\" : {}, \"operations\" : [ [ \"illegal\" ] ] }";

    try {
      new MessageImpl( JsonObject.readFrom( json ) );
      fail();
    } catch( IllegalArgumentException exception ) {
      assertThat( exception.getMessage(), containsString( "Failed to read operations" ) );
      assertThat( exception.getCause().getMessage(), containsString( "Could not read operation" ) );
    }
  }

  @Test
  public void testGetHead() {
    String json = "{ \"head\": { \"foo\" : 23 }, \"operations\": [] }";
    MessageImpl message = new MessageImpl( JsonObject.readFrom( json ) );

    assertEquals( new JsonObject().add( "foo", 23 ), message.getHead() );
  }

  @Test
  public void testGetHead_isModifiable() {
    String json = "{ \"head\": { \"foo\" : 23 }, \"operations\": [] }";
    MessageImpl message = new MessageImpl( JsonObject.readFrom( json ) );

    message.getHead().set( "foo", 42 );

    assertEquals( new JsonObject().add( "foo", 42 ), message.getHead() );
  }

  @Test
  public void testGetHead_withEmptyMessage() {
    String json = "{ \"head\": {}, \"operations\": [] }";
    MessageImpl message = new MessageImpl( JsonObject.readFrom( json ) );

    assertEquals( new JsonObject(), message.getHead() );
  }

  @Test
  public void testGetOperations() {
    String json = "{ \"head\" : {}, \"operations\" : ["
                + "[ \"set\", \"w3\", { \"foo\" : 23 } ],"
                + "[ \"call\", \"w4\", \"method\", { \"bar\" : 42 } ],"
                + "[ \"notify\", \"w3\", \"widgetSelected\", {} ]"
                + "] }";
    MessageImpl message = new MessageImpl( JsonObject.readFrom( json ) );

    List<Operation> operations = message.getOperations();

    assertEquals( 3, operations.size() );
  }

  @Test
  public void testGetOperations_isModifiable() {
    String json = "{ \"head\" : {}, \"operations\" : ["
        + "[ \"set\", \"w3\", { \"foo\" : 23 } ],"
        + "[ \"call\", \"w4\", \"method\", { \"bar\" : 42 } ]"
        + "] }";
    MessageImpl message = new MessageImpl( JsonObject.readFrom( json ) );

    message.getOperations().remove( 1 );

    assertEquals( 1, message.getOperations().size() );
  }

  @Test
  public void testGetOperations_withEmptyMessage() {
    String json = "{ \"head\" : {}, \"operations\" : [] }";
    MessageImpl message = new MessageImpl( JsonObject.readFrom( json ) );

    List<Operation> operations = message.getOperations();

    assertTrue( operations.isEmpty() );
  }

  @Test
  public void testToString_returnsValidJson() {
    String json = "{ \"head\" : {}, \"operations\" : ["
        + "[ \"set\", \"w3\", { \"foo\" : 23 } ]"
        + "] }";
    MessageImpl message = new MessageImpl( JsonObject.readFrom( json ) );

    String string = message.toString();

    assertEquals( JsonObject.readFrom( json ), JsonObject.readFrom( string ) );
  }

}
