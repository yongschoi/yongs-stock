package yongs.temp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import yongs.temp.dao.StockRepository;
import yongs.temp.model.Stock;

@Service
public class StockService {
	private static final Logger logger = LoggerFactory.getLogger(StockService.class);
	
	@Autowired
    StockRepository repo;
	
	public Stock getStock(String code) {
		return repo.findByCode(code);
	}
	
	public Stock addQty(Stock s) {
		logger.debug("flex-stock|StockService|upQty({})", s);
		Stock stock = repo.findByCode(s.getCode());
		int newQty = stock.getQty() + s.getQty();
		stock.setQty(newQty);
		
		return repo.save(stock);
	}
}