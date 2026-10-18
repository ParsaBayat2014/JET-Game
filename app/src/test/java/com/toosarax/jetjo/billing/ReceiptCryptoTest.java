package com.toosarax.jetjo.billing;
import org.junit.Test;
import static org.junit.Assert.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
public class ReceiptCryptoTest {
    private KeyPair key()throws Exception{KeyPairGenerator g=KeyPairGenerator.getInstance("RSA");g.initialize(2048);return g.generateKeyPair();}
    private byte[] sign(KeyPair k,String s)throws Exception{Signature x=Signature.getInstance("SHA1withRSA");x.initSign(k.getPrivate());x.update(s.getBytes(StandardCharsets.UTF_8));return x.sign();}
    @Test public void signedReceiptAccepted()throws Exception{KeyPair k=key();String d="{\"productId\":\"neon_pack_aurora\"}";assertTrue(ReceiptCrypto.verify(k.getPublic().getEncoded(),d,sign(k,d)));}
    @Test public void tamperedProductRejected()throws Exception{KeyPair k=key();assertFalse(ReceiptCrypto.verify(k.getPublic().getEncoded(),"solar",sign(k,"aurora")));}
    @Test public void otherDeveloperKeyRejected()throws Exception{KeyPair k=key();assertFalse(ReceiptCrypto.verify(key().getPublic().getEncoded(),"receipt",sign(k,"receipt")));}
    @Test public void missingOrInvalidSignatureFailsClosed(){assertFalse(ReceiptCrypto.verify(null,"receipt",null));assertFalse(ReceiptCrypto.verify(new byte[]{1},"receipt",new byte[]{2}));}
}

