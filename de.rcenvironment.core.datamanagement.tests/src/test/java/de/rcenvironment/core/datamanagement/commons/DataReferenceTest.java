/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.net.URI;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;

/**
 * Test cases for {@link DataReference}.
 * 
 * @author Juergen Klein
 * @author Doreen Seider
 */
public class DataReferenceTest {

    private DataReference dataReference;

    private InstanceNodeSessionId pi;

    private URI location;

    // /**
    // * Set up.
    // *
    // * @throws Exception if an error occurs.
    // */
    // @Before
    // public void setUp() throws Exception {
    // pi = NodeIdentifierFactory.fromHostAndNumberString("horst:3");
    // dataReference = new DataReference(DataReferenceType.fileObject, UUID.randomUUID(), pi, location);
    // location = new URI("ftp://url");
    // }
    //
    // /** Test. */
    // @Test
    // public void testHashCode() {
    // assertNotNull(dataReference.hashCode());
    // assertTrue(dataReference.hashCode() == dataReference.hashCode());
    // assertTrue(dataReference.hashCode() != new DataReference(DataReferenceType.fileObject, UUID.randomUUID(), pi, location).hashCode());
    // }
    //
    // /** Test. */
    // @Test
    // public void testConstructor() {
    // assertTrue(new DataReference(DataReferenceType.fileObject, UUID.randomUUID(), pi, location) instanceof DataReference);
    // }
    //
    // /** Test. */
    // @Test
    // public void testClone() {
    // assertEquals(dataReference, dataReference.clone());
    // DataReference dr = new DataReference(DataReferenceType.fileObject, UUID.randomUUID(), pi, location);
    // assertEquals(dr, dr.clone());
    //
    // }
    //
    // /** Test. */
    // @Test
    // public void testEquals() {
    // assertTrue(dataReference.equals(dataReference));
    // assertFalse(dataReference.equals(new DataReference(DataReferenceType.fileObject, UUID.randomUUID(), pi, location)));
    // DataReference dataReferenceClone = dataReference.clone();
    // assertTrue(dataReference.equals(dataReferenceClone));
    // DataReference dr = new DataReference(DataReferenceType.fileObject, UUID.randomUUID(), pi, location);
    // assertFalse(dr.equals(dataReference));
    // }
    //
    // /** Test. */
    // @Test
    // public void testGetGuid() {
    // UUID uuid = UUID.randomUUID();
    // DataReference dr = new DataReference(DataReferenceType.fileObject, uuid, pi, location);
    // assertNotNull(dr.getIdentifier());
    // assertEquals(uuid, dr.getIdentifier());
    // }
    //
    // /** Test. */
    // @Test
    // public void testGetLocation() {
    // assertNotNull(dataReference.getLocation());
    // assertEquals(location, dataReference.getLocation());
    // }
    //
    // /** Test. */
    // @Test
    // public void testGetNodeIdentifier() {
    // assertEquals(pi, dataReference.getNodeIdentifier());
    // }
    //
    // /** Test. */
    // @Test
    // public void testGetType() {
    // assertEquals(DataReferenceType.fileObject, dataReference.getDataType());
    // }
    //
    // /** Test. */
    // @Test
    // public void testToString() {
    // assertNotNull(dataReference.toString());
    // }

}
