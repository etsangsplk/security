/*******************************************************************************
 * Copyright 2016 Universidad Politécnica de Madrid UPM
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.universAAL.security.cryptographic.services;

import java.util.ArrayList;

import org.universAAL.middleware.bus.junit.BusTestCase;
import org.universAAL.middleware.owl.ManagedIndividual;
import org.universAAL.middleware.owl.OntologyManagement;
import org.universAAL.middleware.rdf.Resource;
import org.universAAL.middleware.service.CallStatus;
import org.universAAL.middleware.service.DefaultServiceCaller;
import org.universAAL.middleware.service.ServiceRequest;
import org.universAAL.middleware.service.ServiceResponse;
import org.universAAL.ontology.cryptographic.AsymmetricEncryption;
import org.universAAL.ontology.cryptographic.CryptographicOntology;
import org.universAAL.ontology.cryptographic.Digest;
import org.universAAL.ontology.cryptographic.DigestService;
import org.universAAL.ontology.cryptographic.EncryptedResource;
import org.universAAL.ontology.cryptographic.Encryption;
import org.universAAL.ontology.cryptographic.EncryptionKey;
import org.universAAL.ontology.cryptographic.EncryptionService;
import org.universAAL.ontology.cryptographic.KeyRing;
import org.universAAL.ontology.cryptographic.MultidestinationEncryptedResource;
import org.universAAL.ontology.cryptographic.SignAndVerifyService;
import org.universAAL.ontology.cryptographic.SignedResource;
import org.universAAL.ontology.cryptographic.SimpleKey;
import org.universAAL.ontology.cryptographic.SymmetricEncryption;
import org.universAAL.ontology.cryptographic.asymmetric.RSA;
import org.universAAL.ontology.cryptographic.digest.MessageDigest;
import org.universAAL.ontology.cryptographic.digest.SecureHashAlgorithm;
import org.universAAL.ontology.cryptographic.symmetric.AES;
import org.universAAL.ontology.cryptographic.symmetric.Blowfish;
import org.universAAL.ontology.cryptographic.symmetric.DES;


/**
 * @author amedrano
 *
 */
public class ITserviceCalls extends BusTestCase {
	
	private static final String NAMESPACE = "http://tests.universAAL.org/CryptoServices#";
	
	private static final String MY_OUTPUT = NAMESPACE +  "ServiceOutput";
	
	private DefaultServiceCaller scaller;

	private ProjectActivator scallee;

	/**{@inheritDoc} */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
//		OntologyManagement.getInstance().register(mc, new DataRepOntology());
//		OntologyManagement.getInstance().register(mc, new ServiceBusOntology());
//    	OntologyManagement.getInstance().register(mc, new UIBusOntology());
//        OntologyManagement.getInstance().register(mc, new LocationOntology());
//		OntologyManagement.getInstance().register(mc, new SysinfoOntology());
//        OntologyManagement.getInstance().register(mc, new ShapeOntology());
//        OntologyManagement.getInstance().register(mc, new PhThingOntology());
//        OntologyManagement.getInstance().register(mc, new SpaceOntology());
//        OntologyManagement.getInstance().register(mc, new VCardOntology());
//    	OntologyManagement.getInstance().register(mc, new ProfileOntology());
//		OntologyManagement.getInstance().register(mc, new MenuProfileOntology());
		OntologyManagement.getInstance().register(mc, new CryptographicOntology());
		
		scallee = new ProjectActivator();
		scallee.start(mc);
		
	}
	
	public void testExecution(){
		scaller = new DefaultServiceCaller(mc);
		
		//Digest tests
		callWDigest(MessageDigest.IND_MD2);
		callWDigest(MessageDigest.IND_MD5);
		callWDigest(SecureHashAlgorithm.IND_SHA);
		callWDigest(SecureHashAlgorithm.IND_SHA256);
		callWDigest(SecureHashAlgorithm.IND_SHA384);
		callWDigest(SecureHashAlgorithm.IND_SHA512);
		
		//Key Generation
		SimpleKey keyAES = simpleKeygen(new AES(),128);
		SimpleKey keyBlow = simpleKeygen(new Blowfish(),128);
		SimpleKey keyDES = simpleKeygen(new DES(),56); 
		KeyRing keyring = keringKeygen(new RSA(),1024);
		
		//Encryption
		encryptionCycle(new AES(), keyAES);
		encryptionCycle(new Blowfish(), keyBlow);
		encryptionCycle(new DES(), keyDES);
		encryptionCycle(new RSA(), keyring);
		
		//digital signature
		signatureCycle(keyring);
		
		//Multidestination Encryption
		KeyRing keyring2 = keringKeygen(new RSA(),1024);
		ArrayList<KeyRing> krl = new ArrayList<KeyRing>();
		krl.add(keyring);
		krl.add(keyring2);
		multidestinationEncryptionCycle(krl);
	}

	private void callWDigest(Digest method) {
		Resource example = RandomResourceGenerator.randomResource();
		
		ServiceRequest sreq = new ServiceRequest(new DigestService(), null);
		sreq.addRequiredOutput(MY_OUTPUT, new String[] {DigestService.PROP_DIGESTED_TEXT});
		sreq.addValueFilter(new String [] {DigestService.PROP_RESOURCE_TO_DIGEST}, example);
		sreq.addValueFilter(new String[] {DigestService.PROP_DIGEST_METHOD}, method);
		
		ServiceResponse sres = scaller.call(sreq);
		assertEquals(CallStatus.succeeded, sres.getCallStatus());
	}
	private SimpleKey simpleKeygen(SymmetricEncryption se, int keylength){
		ServiceRequest sreq = new ServiceRequest(new EncryptionService(), null);
		
		sreq.addValueFilter(new String [] {EncryptionService.PROP_ENCRYPTION}, se );
		sreq.addValueFilter(new String[]{EncryptionService.PROP_ENCRYPTION,SymmetricEncryption.PROP_SIMPLE_KEY,SimpleKey.PROP_KEY_LENGTH}, Integer.valueOf(keylength) );
		//if previous statement is left out, even the propfile having cardinality 0,1 it will not match.
		sreq.addRequiredOutput(MY_OUTPUT, new String[]{EncryptionService.PROP_ENCRYPTION,SymmetricEncryption.PROP_SIMPLE_KEY});
		
		ServiceResponse sres = scaller.call(sreq);
		assertEquals(CallStatus.succeeded, sres.getCallStatus());
		return (SimpleKey) sres.getOutput(MY_OUTPUT).get(0);
	}

	private KeyRing keringKeygen(AsymmetricEncryption ae, int keylength) {
		ServiceRequest sreq = new ServiceRequest(new EncryptionService(), null);
		
		sreq.addValueFilter(new String [] {EncryptionService.PROP_ENCRYPTION}, ae );
		sreq.addValueFilter(new String[]{EncryptionService.PROP_ENCRYPTION,RSA.PROP_KEY_RING,KeyRing.PROP_KEY_LENGTH}, Integer.valueOf(keylength) );
		//if previous statement is left out, even the propfile having cardinality 0,1 it will not match.
		sreq.addRequiredOutput(MY_OUTPUT, new String[]{EncryptionService.PROP_ENCRYPTION,RSA.PROP_KEY_RING});
		
		ServiceResponse sres = scaller.call(sreq);
		assertEquals(CallStatus.succeeded, sres.getCallStatus());
		return (KeyRing) sres.getOutput(MY_OUTPUT).get(0);
		
	}

	private void encryptionCycle(Encryption se, EncryptionKey k) {
		
		String keyprop = null;
		if (se instanceof SymmetricEncryption){
			keyprop = SymmetricEncryption.PROP_SIMPLE_KEY;
		}else {
			keyprop = AsymmetricEncryption.PROP_KEY_RING;
		}
		
		
		Resource clearResource = RandomResourceGenerator.randomResource();
		
		ServiceRequest sreq = new ServiceRequest(new EncryptionService(), null);

		sreq.addValueFilter(new String[] {EncryptionService.PROP_ENCRYPTION, keyprop}, k);
		sreq.addValueFilter(new String[] {EncryptionService.PROP_ENCRYPTION}, se);
		sreq.addValueFilter(new String[] {EncryptionService.PROP_ENCRYPTS}, clearResource);
		sreq.addRequiredOutput(MY_OUTPUT, new String[] {EncryptionService.PROP_ENCRYPTED_RESOURCE});
		
		ServiceResponse sres = scaller.call(sreq);
		assertEquals(CallStatus.succeeded, sres.getCallStatus());
		
		Resource cryptedResource = (Resource) sres.getOutput(MY_OUTPUT).get(0);
		
		System.out.println(serialize(cryptedResource));
		
		// decrypt
		ServiceRequest sreq2 = new ServiceRequest(new EncryptionService(), null);
		
		sreq2.addValueFilter(new String[] {EncryptionService.PROP_ENCRYPTION, keyprop}, k);
		sreq2.addValueFilter(new String[] {EncryptionService.PROP_ENCRYPTION}, se);
		sreq2.addValueFilter(new String[] {EncryptionService.PROP_ENCRYPTED_RESOURCE}, cryptedResource);
		sreq2.addRequiredOutput(MY_OUTPUT, new String[] {EncryptionService.PROP_ENCRYPTS});
		
		sres = scaller.call(sreq2);
		assertEquals(CallStatus.succeeded, sres.getCallStatus());
		
		Resource decryptedResource = (Resource) sres.getOutput(MY_OUTPUT).get(0);
		
		System.out.println(serialize(decryptedResource));
		
		assertTrue(EncryptTest.fullResourceEquals(clearResource, decryptedResource));
	}

	private void signatureCycle(KeyRing keyring) {
		Resource res = RandomResourceGenerator.randomResource();
		
		ServiceRequest sreq = new ServiceRequest(new SignAndVerifyService(), null);
		AsymmetricEncryption ae = new RSA();
		ae.addKeyRing(keyring);

		sreq.addValueFilter(new String [] {SignAndVerifyService.PROP_SIGN}, res );
		sreq.addValueFilter(new String []{SignAndVerifyService.PROP_ASYMMETRIC}, ae);
		sreq.addValueFilter(new String []{SignAndVerifyService.PROP_DIGEST}, SecureHashAlgorithm.IND_SHA256);
		sreq.addRequiredOutput(MY_OUTPUT, new String[]{SignAndVerifyService.PROP_SIGNED_RESOURCE});
		
		ServiceResponse sres = scaller.call(sreq);
		assertEquals(CallStatus.succeeded, sres.getCallStatus());
		SignedResource sr = (SignedResource) sres.getOutput(MY_OUTPUT).get(0);
		
		//System.out.println(serialize(sr));
		
		System.out.println("Verifying");
		
		sreq = new ServiceRequest(new SignAndVerifyService(), null);
		sreq.addValueFilter(new String[]{SignAndVerifyService.PROP_SIGNED_RESOURCE}, sr);
		sreq.addValueFilter(new String []{SignAndVerifyService.PROP_ASYMMETRIC}, ae);
		
		sreq.addRequiredOutput(MY_OUTPUT, new String[]{SignAndVerifyService.PROP_VERIFICATION_RESULT});
		sres = scaller.call(sreq);
		assertEquals(CallStatus.succeeded, sres.getCallStatus());
		Boolean result = (Boolean) sres.getOutput(MY_OUTPUT).get(0);
		assertEquals(Boolean.TRUE, result);
		
		
	}

	private void multidestinationEncryptionCycle(ArrayList<KeyRing> krl) {
		Resource clearResource = RandomResourceGenerator.randomResource();
		
		AsymmetricEncryption ae = new RSA();
		for (KeyRing kr : krl) {
			ae.addKeyRing(kr);
		}
		
		ServiceRequest sreq = new ServiceRequest(new EncryptionService(), null);

		sreq.addValueFilter(new String[] {EncryptionService.PROP_ENCRYPTED_RESOURCE, EncryptedResource.PROP_ENCRYPTION}, new AES());
		sreq.addValueFilter(new String[] {EncryptionService.PROP_ENCRYPTION}, ae);
		sreq.addValueFilter(new String[] {EncryptionService.PROP_ENCRYPTS}, clearResource);
		sreq.addRequiredOutput(MY_OUTPUT, new String[] {EncryptionService.PROP_ENCRYPTED_RESOURCE});
		
		ServiceResponse sres = scaller.call(sreq);
		assertEquals(CallStatus.succeeded, sres.getCallStatus());
		
		Resource cryptedResource = (Resource) sres.getOutput(MY_OUTPUT).get(0);
		

		assertTrue(ManagedIndividual.checkMembership(MultidestinationEncryptedResource.MY_URI, cryptedResource));
		
		System.out.println(serialize(cryptedResource));
		
		sreq = new ServiceRequest(new EncryptionService(), null);
		
		sreq.addValueFilter(new String[] {EncryptionService.PROP_ENCRYPTION, AsymmetricEncryption.PROP_KEY_RING}, krl.get(0));
		sreq.addValueFilter(new String[] {EncryptionService.PROP_ENCRYPTED_RESOURCE}, cryptedResource);
		sreq.addRequiredOutput(MY_OUTPUT, new String[] {EncryptionService.PROP_ENCRYPTS});
		
		sres = scaller.call(sreq);
		assertEquals(CallStatus.succeeded, sres.getCallStatus());
		
		Resource decryptedResource = (Resource) sres.getOutput(MY_OUTPUT).get(0);
						
		assertTrue(EncryptTest.fullResourceEquals(clearResource, decryptedResource));
		
	}
}
