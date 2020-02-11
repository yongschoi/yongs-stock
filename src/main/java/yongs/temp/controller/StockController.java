package yongs.temp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import yongs.temp.model.Stock;
import yongs.temp.service.StockService;

@RestController
@RequestMapping("/stock")
public class StockController {
    private static final Logger logger = LoggerFactory.getLogger(StockController.class);	

    @Autowired
    StockService service;
    
    @GetMapping("/code/{code}")
    public Stock getStock(@PathVariable("code") String code) throws Exception{
    	logger.debug("yongs-stock|StockController|getStock()");    	
        return service.getStock(code);
    }
}