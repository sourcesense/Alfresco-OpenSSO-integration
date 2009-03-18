package com.sourcesense.alfresco.opensso;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OpenSSOClientAdapterTest {

	private OpenSSOClientAdapter openSSOClientAdapter;
	public static HashSet<String> cngroups;

	@Before
	public void setUp() throws Exception {
		openSSOClientAdapter = new OpenSSOClientAdapter();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void shouldExtractGroups() throws Exception {
		cngroups = new HashSet<String>();
		cngroups.add("cn=administration,ou=groups,dc=opensso,dc=java,dc=net");
		cngroups.add("cn=marketing,ou=groups,dc=opensso,dc=java,dc=net");
		cngroups.add("cn=RH,ou=groups,dc=opensso,dc=java,dc=net");
		
		List<String> groupNames = openSSOClientAdapter.extractGroupNameFromFQGroup(cngroups);
		
		assertEquals(3, groupNames.size());
		assertTrue(groupNames.contains("administration"));
		assertTrue(groupNames.contains("marketing"));
		assertTrue(groupNames.contains("RH"));
		
	}

}
