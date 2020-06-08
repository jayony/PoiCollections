/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.poifs.crypt.dsig;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.HashAlgorithm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;

import javax.crypto.Cipher;
//import org.ietf.jgss.GSSException;
//import org.ietf.jgss.Oid;

/* package */ class DigestOutputStream extends OutputStream {
    final HashAlgorithm algo;
    final PrivateKey key;
    private MessageDigest md;

    DigestOutputStream(final HashAlgorithm algo, final PrivateKey key) {
        this.algo = algo;
        this.key = key;
    }

    public void init() throws GeneralSecurityException {
        if (isMSCapi(key)) {
            // see https://stackoverflow.com/questions/39196145 for problems with SunMSCAPI
            // and why we can't sign the calculated digest
            throw new EncryptedDocumentException(
                "Windows keystore entries can't be signed with the "+algo+" hash. Please "+
                "use one digest algorithm of sha1 / sha256 / sha384 / sha512.");
        }
        md = CryptoFunctions.getMessageDigest(algo);
    }
    
    @Override
    public void write(final int b) throws IOException {
        md.update((byte)b);
    }

    @Override
    public void write(final byte[] data, final int off, final int len) throws IOException {
        md.update(data, off, len);
    }

    public byte[] sign() throws IOException, GeneralSecurityException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(getHashMagic());
        bos.write(md.digest());

        final Cipher cipher = CryptoFunctions.getCipher(key, CipherAlgorithm.rsa
            , ChainingMode.ecb, null, Cipher.ENCRYPT_MODE, "PKCS1Padding");
        return cipher.doFinal(bos.toByteArray());
    }
    
    static boolean isMSCapi(final PrivateKey key) {
        return key != null && key.getClass().getName().contains("mscapi");
    }


    /**
     * Each digest method has its own ASN1 header
     *
     * @return the ASN1 header bytes for the signatureValue / digestInfo
     * 
     * @see <a href="https://tools.ietf.org/html/rfc2313#section-10.1.2">Data encoding</a>
     */
    byte[] getHashMagic() {
        // in an earlier release the hashMagic (aka DigestAlgorithmIdentifier) contained only
        // an object identifier, but to conform with the header generated by the
        // javax-signature API, the empty <associated parameters> are also included
        try {
            final byte[] oidBytes = new byte[0];//new Oid(algo.rsaOid).getDER();

            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(0x30);
            bos.write(algo.hashSize+oidBytes.length+6);
            bos.write(0x30);
            bos.write(oidBytes.length+2);
            bos.write(oidBytes);
            bos.write(new byte[] {5,0,4});
            bos.write(algo.hashSize);
            
            return bos.toByteArray();
        } catch (/*GSSException|IO*/Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
