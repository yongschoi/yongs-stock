package yongs.temp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import yongs.temp.dao.StockRepository;
import yongs.temp.model.Stock;

@Service
public class StockService {
	@Autowired
    StockRepository repo;
	
	public Stock getStock(String code) {
		return repo.findByCode(code);
	}
	
	public boolean deductQty(Stock stock) {
		Stock entity = repo.findByCode(stock.getCode());
		int newQty = entity.getQty() - stock.getQty();
		if(newQty < 0) {
			// 재고 부족
			return false;
		} else {
			entity.setQty(newQty);
			repo.save(entity);
			return true;
		}
	}
}
