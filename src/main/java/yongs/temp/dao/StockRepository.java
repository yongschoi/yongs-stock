package yongs.temp.dao;

import org.springframework.data.mongodb.repository.MongoRepository;

import yongs.temp.model.Stock;

public interface StockRepository extends MongoRepository<Stock, String> {
	public Stock findByCode(final String code);
}