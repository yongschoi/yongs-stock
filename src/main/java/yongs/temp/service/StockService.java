package yongs.temp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import yongs.temp.dao.StockRepository;
import yongs.temp.model.Stock;
import yongs.temp.vo.Order;

@Service
public class StockService {
	private static final Logger logger = LoggerFactory.getLogger(StockService.class);
	// for sender
	private static final String STOCK_PAYMENT_EVT = "stock-to-payment";
	private static final String STOCK_ROLLBACK_EVT = "stock-rollback";
	
	// for listener
	private static final String ORDER_STOCK_EVT = "order-to-stock";
	private static final String ROLLBACK_EVT = "payment-rollback, delivery-rollback";
	
	@Autowired
    KafkaTemplate<String, String> kafkaTemplate;
	
	@Autowired
    StockRepository repo;
	
	public Stock getStock(String code) {
		return repo.findByCode(code);
	}

	@KafkaListener(topics = ORDER_STOCK_EVT)
	public void deductQty(String orderStr) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		Order order = mapper.readValue(orderStr, Order.class);
		Stock entity = repo.findByCode(order.getProduct().getCode());
		int newQty = entity.getQty() - order.getQty();
		if(newQty < 0) {
			// 재고 부족
			logger.info(">>>>> 재고가 부족합니다 >>>>>> " + newQty);
			kafkaTemplate.send(STOCK_ROLLBACK_EVT, orderStr);
		} else {
			// 차감한 재고수량 업데이트
			entity.setQty(newQty);
			repo.save(entity);
			logger.info(">>>>> 재고 업데이트 완료 >>>>>> " + newQty);
			// to payment
			kafkaTemplate.send(STOCK_PAYMENT_EVT, orderStr);
		}
	}
	
	@KafkaListener(topics = ROLLBACK_EVT)
	public void rollback(String orderStr) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		Order order = mapper.readValue(orderStr, Order.class);
		
		// 재고 원상복구
		Stock entity = repo.findByCode(order.getProduct().getCode());
		int oldQty = entity.getQty() + order.getQty();
		entity.setQty(oldQty);
		repo.save(entity);
		logger.info("Stock Code [" + entity.getCode() + "] Rollback !!!");
	}

}
