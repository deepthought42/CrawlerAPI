package com.looksee.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.audit.PaletteColor;
import com.looksee.models.repository.PaletteColorRepository;

@Service
public class PaletteColorService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PaletteColorService.class);

	@Autowired
	private PaletteColorRepository palette_color_repo;
	
	public PaletteColor save(PaletteColor palette_color) {
		assert palette_color != null;
		Optional<PaletteColor> palette_color_record = palette_color_repo.findByKey(palette_color.getKey());
		if(palette_color_record.isPresent()) {
			return palette_color_record.get();
		}
		return palette_color_repo.save(palette_color);
	}

	public List<PaletteColor> saveAll(List<PaletteColor> palette_colors) {
		assert palette_colors != null;

		List<PaletteColor> palette = new ArrayList<>();
		for(PaletteColor color : palette_colors) {
			palette.add(palette_color_repo.save(color));
		}
		return palette;
	}
}
