package io.apiman.plugins.lsat_policy;

import com.github.nitram509.jmacaroons.GeneralCaveatVerifier;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.message.VerifyMessageResponse;

import java.io.ByteArrayInputStream;

public class ChallengeVerifier implements GeneralCaveatVerifier {

    private LsatConfiguration config;

    public ChallengeVerifier(LsatConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean verifyCaveat(String s) {
        if (s.startsWith("challenge")) {
            if (!s.endsWith(":")) {
                try {
                    String[] token = parse(s);
                    System.out.println(String.format("token: msg = %s, pub = %s, sign = %s", token[0], token[1], token[2]));
                    byte[] cert = config.getTls().getBytes();
                    SslContext sslContext =
                            GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
                                    .trustManager(new ByteArrayInputStream(cert))
                                    .build();
                    SynchronousLndAPI synchronousLndAPI = new SynchronousLndAPI(config.getHostx(), 10019, sslContext, () -> config.getMacaroon());
                    VerifyMessageResponse verifyMessageResponse = synchronousLndAPI.verifyMessage(token[0].getBytes(), token[2]);

                    return verifyMessageResponse.getValid();
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            } else {
                return true;
            }
        }

        return false;
    }

    public static String[] parse(String caveat) {
        String[] strings = caveat.split(":");
        String msg = strings[0].split("=")[1];
        String pubkey = strings[1];
        String signature = strings[2];

        return new String[]{msg, pubkey, signature};
    }
}
