package com.example.ogani.service.impl;

import com.example.ogani.config.VNPayConfig;
import com.example.ogani.entity.Order;
import com.example.ogani.entity.VNPayTransaction;
import com.example.ogani.exception.NotFoundException;
import com.example.ogani.model.payment.PaymentSession;
import com.example.ogani.repository.VNPayTransactionRepository;
import com.example.ogani.service.RedisService;
import com.example.ogani.service.VNPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class VNPayServiceImpl implements VNPayService {

    @Autowired
    private VNPayConfig vnPayConfig;

    @Autowired
    private VNPayTransactionRepository vnPayTransactionRepository;

    @Autowired
    private RedisService redisService;

    @Override
    public String createPaymentUrl(Order order, String ipAddress) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = getRandomNumber(8);
        String vnp_IpAddr = ipAddress;
        String vnp_TmnCode = vnPayConfig.getTmnCode();
        String orderType = "other";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(order.getTotalPrice() * 100));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang: " + order.getId());
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnPayConfig.getUrl() + "?" + queryUrl;
    }

    @Override
    public Map<String, String> validatePaymentResponse(Map<String, String> response) {
        Map<String, String> result = new HashMap<>();
        try {
            String vnp_SecureHash = response.get("vnp_SecureHash");
            response.remove("vnp_SecureHash");
            response.remove("vnp_SecureHashType");

            List<String> fieldNames = new ArrayList<>(response.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = response.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append("=");
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append("&");
                    }
                }
            }

            String calculatedHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
            if (calculatedHash.equals(vnp_SecureHash)) {
                result.put("status", "success");
                result.put("message", "Valid VNPay Response");
            } else {
                result.put("status", "error");
                result.put("message", "Invalid Signature");
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Override
    public void saveTransaction(Map<String, String> vnpayResponse, Order order) {
        VNPayTransaction transaction = new VNPayTransaction();
        transaction.setVnpTxnRef(vnpayResponse.get("vnp_TxnRef"));
        transaction.setVnpOrderInfo(vnpayResponse.get("vnp_OrderInfo"));
        transaction.setAmount(Long.parseLong(vnpayResponse.get("vnp_Amount")));
        transaction.setBankCode(vnpayResponse.get("vnp_BankCode"));
        transaction.setCardType(vnpayResponse.get("vnp_CardType"));
        transaction.setPaymentTime(LocalDateTime.now());
        transaction.setTransactionStatus(vnpayResponse.get("vnp_TransactionStatus"));
        transaction.setSecureHash(vnpayResponse.get("vnp_SecureHash"));
        transaction.setOrder(order);

        vnPayTransactionRepository.save(transaction);
    }

    @Override
    public VNPayTransaction getTransactionByOrderId(String orderId) {
        return null;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            sha512_HMAC.init(secret_key);
            byte[] hash = sha512_HMAC.doFinal(data.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public PaymentSession createPaymentSession(Order order) {
        // Generate unique session ID
        String sessionId = UUID.randomUUID().toString();

        // Create payment session
        PaymentSession session = new PaymentSession();
        session.setSessionId(sessionId);
        session.setOrderId(order.getId());
        session.setPaymentMethod("VNPAY");
        session.setStatus("PENDING");
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        // Store order in Redis
        redisService.storeOrderForPayment(order, sessionId);

        return session;
    }

    @Override
    public Order getOrderBySessionId(String sessionId) {
        return redisService.getOrderByPaymentSessionId(sessionId)
                .orElseThrow(() -> new NotFoundException("Payment session not found or expired: " + sessionId));
    }

    @Override
    public void completePaymentSession(String sessionId, String status) {
        // Remove order from Redis
        redisService.removeOrder(sessionId);
    }
}