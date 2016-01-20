package io.opentraffic.engine.app.util;

/**
 * Created by dbenoff on 1/19/16.
 */

import org.apache.commons.io.Charsets;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;

import java.util.Base64;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

public class PasswordUtil {

    private static final DigestRandomGenerator generator = new DigestRandomGenerator(new SHA3Digest(512));
    private static final byte[] salt = Base64.getEncoder().encode("HelloWorld".getBytes(Charsets.UTF_8));


    private PasswordUtil() {

    }

    public static String hash(String plainPassword) {
        return hash(plainPassword, salt, 512, 101501);
    }

    public static String hash(String plainPassword, byte[] salt) {
        return hash(plainPassword, salt, 512, 101501);
    }

    public static String hash(String plainPassword, byte[] salt, int keyLength, int iterations) {
        checkArgument(!isNullOrEmpty(plainPassword), "password can not be empty or null");
        checkArgument(keyLength > 0, "the key length must be greater than 0");
        checkArgument(iterations >= 0, "the number of iterations must be positive");

        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
                        plainPassword.toCharArray()),
                salt,
                iterations);

        return format("%s|%s",
                encode(salt),
                encode(((KeyParameter) generator.generateDerivedParameters(keyLength)).getKey()));
    }

    public static boolean verify(String plainPassword, String hash) {
        checkArgument(!isNullOrEmpty(plainPassword));
        checkArgument(!isNullOrEmpty(hash));
        return hash(plainPassword, decode(extractSalt(hash))).equals(hash);
    }

    private static byte[] salt(int count) {
        byte[] salt = new byte[count];
        generator.nextBytes(salt);
        return salt;
    }

    private static String encode(byte[] input) {
        return Base64.getEncoder().encodeToString(input);
    }

    private static byte[] decode(String input) {
        return Base64.getDecoder().decode(input.getBytes(UTF_8));
    }

    private static String extractSalt(String input) {
        return input.substring(0, input.indexOf("|"));
    }

}