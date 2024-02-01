package site.minnan.robotmanage.infrastructure.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.WebAuthUser;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT相关操作
 *
 * @author Minnan on 2020/12/16
 */
@Component
public class JwtUtil {

    @Value("${jwt.expiration}")
    private long JWT_TOKEN_VALIDITY;

    @Value("${jwt.secret}")
    private String secret;

    //从token中解析用户名
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    //从token中解析过期时间
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    //解析token
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        Date now = new Date();
        return expiration.before(now);
    }

    public String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    public String generateToken(WebAuthUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("nickName", user.getNickName());
        claims.put("id", user.getId());
        claims.put("stamp", user.getPasswordStamp());
        return doGenerateToken(claims, user.getUsername());
    }

//    public String generateToken(JwtUser jwtUser) {
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("realName", jwtUser.getRealName());
//        claims.put("id", jwtUser.getId());
//        claims.put("stamp", jwtUser.getPasswordStamp());
//        return doGenerateToken(claims, jwtUser.getUsername());
//    }

    public Boolean validateToken(String token, WebAuthUser userDetails) {
        String username = getClaimFromToken(token, Claims::getSubject);
        String stamp = getClaimFromToken(token, e -> e.get("stamp", String.class));
        return (username.equals(userDetails.getUsername()) && stamp != null && stamp.equals(userDetails.getPasswordStamp()) && !isTokenExpired(token));
    }
}
