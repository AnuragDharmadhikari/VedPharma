package org.ved.crm.stockist;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.common.exception.ResourceNotFoundException;
import org.ved.crm.user.User;
import org.ved.crm.user.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockistService {

    private final StockistRepository stockistRepository;
    private final StockistMapper stockistMapper;
    private final UserRepository userRepository;

    public List<StockistDto> getAllActiveStockists(){
        return stockistRepository.findAllActive()
                .stream()
                .map(stockistMapper::toDto)
                .toList();
    }

    public StockistDto getStockistById(UUID id){
        return stockistRepository.findByIdWithDetails(id)
                .map(stockistMapper::toDto)
                .orElseThrow(()->new ResourceNotFoundException("Stockist","id",id));
    }

    public List<StockistDto> getStockistsByRep(UUID repId){
        return stockistRepository.findByAssignedRepId(repId)
                .stream()
                .map(stockistMapper::toDto)
                .toList();
    }

    @Transactional
    public StockistDto createStockist(CreateStockistRequest request){
        if(request.gstin() !=null && stockistRepository.existsByGstin(request.gstin())){
            throw new IllegalArgumentException("Stockist with GSTIN already exists: " + request.gstin());
        }

        User rep = userRepository.findById(request.assignedRepId())
                .orElseThrow(()->new ResourceNotFoundException("User","id",request.assignedRepId()));

        Stockist stockist = Stockist.builder()
                .assignedRep(rep)
                .firmName(request.firmName())
                .ownerName(request.ownerName())
                .gstin(request.gstin())
                .state(request.state())
                .city(request.city())
                .address(request.address())
                .phone(request.phone())
                .build();

        stockistRepository.save(stockist);
        return stockistMapper.toDto(stockistRepository.findByIdWithDetails(stockist.getId()).orElseThrow());
    }

    @Transactional
    public StockistDto updateStockist(UUID id, UpdateStockistRequest request){
        Stockist stockist = stockistRepository.findByIdWithDetails(id)
                .orElseThrow(()->new ResourceNotFoundException("Stockist","id",id));

        if (request.gstin() != null &&
                !request.gstin().equals(stockist.getGstin()) &&
                stockistRepository.existsByGstin(request.gstin())) {
            throw new IllegalArgumentException(
                    "Stockist with GSTIN already exists: " + request.gstin());
        }

        User rep = userRepository.findById(request.assignedRepId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", request.assignedRepId()));

        stockist.setAssignedRep(rep);
        stockist.setFirmName(request.firmName());
        stockist.setOwnerName(request.ownerName());
        stockist.setGstin(request.gstin());
        stockist.setState(request.state());
        stockist.setCity(request.city());
        stockist.setAddress(request.address());
        stockist.setPhone(request.phone());

        if(request.isActive()!=null){
            stockist.setActive(request.isActive());
        }

        stockistRepository.save(stockist);
        return stockistMapper.toDto(
                stockistRepository.findByIdWithDetails(id).orElseThrow());

    }

    @Transactional
    public void deactivateStockist(UUID id) {
        Stockist stockist = stockistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stockist", "id", id));
        stockist.setActive(false);
        stockistRepository.save(stockist);
    }


}
