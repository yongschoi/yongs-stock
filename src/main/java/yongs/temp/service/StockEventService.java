package yongs.temp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import yongs.temp.dao.StockRepository;
import yongs.temp.model.Stock;
import yongs.temp.vo.Order;
import yongs.temp.vo.Product;

@Service
public class StockEventService {
	private static final Logger logger = LoggerFactory.getLogger(StockEventService.class);
	// for sender
	private static final String STOCK_PAYMENT_EVT = "stock-to-payment";
	private static final String STOCK_ROLLBACK_EVT = "stock-rollback";
	
	private static final String STOCK_NEW_ROLLBACK_SND = "stock-new-rollback";
	
	// for listener
	private static final String ORDER_STOCK_EVT = "order-to-stock";	
	// 신규 product 생성시 stock에 product 정보를 생성 (qty:0)
	private static final String PRODUCT_STOCK_NEW_LSN = "product-stock-new";
	
	@Autowired
    KafkaTemplate<String, String> kafkaTemplate;
	
	@Autowired
    StockRepository repo;

	@KafkaListener(topics = ORDER_STOCK_EVT)
	public void deductQty(String orderStr, Acknowledgment ack) {
		logger.debug("yongs-stock|StockEventService|deductQty()");
		ObjectMapper mapper = new ObjectMapper();
		try {
			Order order = mapper.readValue(orderStr, Order.class);
			Stock entity = repo.findByCode(order.getProduct().getCode());
			int newQty = entity.getQty() - order.getQty();
			if(newQty >= 0) {
				// 차감한 재고수량 업데이트
				entity.setQty(newQty);
				repo.save(entity);
				// to payment
				kafkaTemplate.send(STOCK_PAYMENT_EVT, orderStr);
				logger.debug("[STOCK to PAYMENT(재고업데이트)] Order No [" + order.getNo() + "]");
			} else {
				// 재고 부족
				throw new Exception();
			}
		} catch (Exception e) {
			kafkaTemplate.send(STOCK_ROLLBACK_EVT, orderStr);
			logger.debug("[STOCK Exception(재고부족)]");
		}
		// 성공하든 실패하든 상관없이
		ack.acknowledge();
	}
	
	@KafkaListener(topics = {"payment-rollback", "delivery-rollback"})
	public void rollback(String orderStr, Acknowledgment ack) {
		logger.debug("yongs-stock|StockEventService|rollback()");
		ObjectMapper mapper = new ObjectMapper();
		try {
			Order order = mapper.readValue(orderStr, Order.class);
			// 재고 원상복구
			Stock entity = repo.findByCode(order.getProduct().getCode());
			int oldQty = entity.getQty() + order.getQty();
			entity.setQty(oldQty);
			repo.save(entity);
			// 실패하면 commit되지 않으므로 성공할때 까지 수행한다.
			ack.acknowledge();
			logger.debug("[STOCK Rollback] Order No [" + order.getNo() + "]");
		} catch (Exception e) { 
			e.printStackTrace();
		} 
	}
	
	@KafkaListener(topics = PRODUCT_STOCK_NEW_LSN)
	public void create(String productStr, Acknowledgment ack) {
		logger.debug("yongs-stock|StockEventService|create()");
		ObjectMapper mapper = new ObjectMapper();
		try {
			Product product = mapper.readValue(productStr, Product.class);
			
			Stock entity = new Stock();
			entity.setCode(product.getCode());
			entity.setName(product.getName());
			entity.setQty(0);
			logger.debug("code: " + product.getCode() + " name: " + product.getName());
			
			repo.save(entity);			
		} catch (Exception e) {
			kafkaTemplate.send(STOCK_NEW_ROLLBACK_SND, productStr);
			logger.debug("[STOCK Exception(Stock new 실패)]");
		}
		// 성공하든 실패하든 상관없이
		ack.acknowledge();
	}
}