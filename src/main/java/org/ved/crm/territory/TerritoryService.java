package org.ved.crm.territory;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ved.crm.common.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TerritoryService {

    private final TerritoryRepository territoryRepository;
    private final TerritoryMapper territoryMapper;

    public List<TerritoryDto> getAllTerritories(){
        return territoryRepository.findAll()
                .stream()
                .map(territoryMapper::toDto)
                .toList();
    }

    public TerritoryDto getTerritoryById(UUID id){
        Territory territory = territoryRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Territory","id",id));
        return territoryMapper.toDto(territory);
    }

    @Transactional
    public TerritoryDto createTerritory(CreateTerritoryRequest request) {
        Territory territory = Territory.builder()
                .name(request.name())
                .state(request.state())
                .zone(request.zone())
                .build();

        Territory saved = territoryRepository.save(territory);
        return territoryMapper.toDto(
                territoryRepository.findById(saved.getId()).orElseThrow());
    }

    @Transactional
    public TerritoryDto updateTerritory(UUID id, UpdateTerritoryRequest request) {
        Territory territory = territoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Territory", "id", id));

        territory.setName(request.name());
        territory.setState(request.state());
        territory.setZone(request.zone());

        if (request.isActive() != null) {
            territory.setActive(request.isActive());
        }

        territoryRepository.save(territory);
        return territoryMapper.toDto(
                territoryRepository.findById(id).orElseThrow());
    }

    @Transactional
    public void deactivateTerritory(UUID id) {
        Territory territory = territoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Territory", "id", id));
        territory.setActive(false);
        territoryRepository.save(territory);
    }
}
