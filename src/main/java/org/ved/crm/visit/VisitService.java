package org.ved.crm.visit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.common.exception.ResourceNotFoundException;
import org.ved.crm.doctor.Doctor;
import org.ved.crm.doctor.DoctorRepository;
import org.ved.crm.product.Product;
import org.ved.crm.product.ProductRepository;
import org.ved.crm.user.User;
import org.ved.crm.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitProductRepository visitProductRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final ProductRepository productRepository;
    private final VisitMapper visitMapper;

    public List<VisitDto> getAllVisits(){
        return visitRepository.findAllWithDetails()
                .stream()
                .map(visitMapper::toDto)
                .toList();
    }

    public VisitDto getVisitById(UUID id){
        Visit visit = visitRepository.findByIdWithDetails(id)
                .orElseThrow(()->new ResourceNotFoundException("Visit","id",id));
        return visitMapper.toDto(visit);
    }

    public List<VisitDto> getVisitsByDoctor(UUID doctorId){
        return visitRepository.findByDoctorIdWithDetails(doctorId)
                .stream()
                .map(visitMapper::toDto)
                .toList();
    }

    public List<VisitDto> getVisitsByRep(UUID repId) {
        return visitRepository.findByRepIdWithDetails(repId)
                .stream()
                .map(visitMapper::toDto)
                .toList();
    }

    @Transactional
    public VisitDto createVisit(CreateVisitRequest request){
        User rep = userRepository.findById(request.repId())
                .orElseThrow(()->new ResourceNotFoundException("User","id",request.repId()));

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(()->new ResourceNotFoundException("Doctor","id",request.doctorId()));

        Visit visit = Visit.builder()
                .rep(rep)
                .doctor(doctor)
                .visitDate(request.visitDate())
                .status(request.status())
                .notes(request.notes())
                .build();

        Visit saved = visitRepository.save(visit);

        if(request.products()!=null && !request.products().isEmpty()){
            List<VisitProduct> visitProducts = buildVisitProducts(request.products(),saved);
            visitProductRepository.saveAll(visitProducts);
            saved.setVisitProducts(visitProducts);
        }

        return visitMapper.toDto(visitRepository.findByIdWithDetails(saved.getId()).orElseThrow());
    }

    @Transactional
    public VisitDto updateVisit(UUID id, UpdateVisitRequest request){
        Visit visit = visitRepository.findByIdWithDetails(id)
                .orElseThrow(()->new ResourceNotFoundException("Visit","id",id));
        if(request.status()!=null){
            visit.setStatus(request.status());
        }
        if(request.notes()!=null){
            visit.setNotes(request.notes());
        }
        if(request.products()!=null){
            visit.getVisitProducts().clear();
            List<VisitProduct> newProducts = buildVisitProducts(request.products(),visit);
            visit.getVisitProducts().addAll(newProducts);
        }
        Visit saved = visitRepository.save(visit);
        return visitMapper.toDto(
                visitRepository.findByIdWithDetails(saved.getId()).orElseThrow()
        );
    }

    private List<VisitProduct> buildVisitProducts(List<VisitProductRequest> requests,Visit visit){
        List<VisitProduct> result = new ArrayList<>();
        for(VisitProductRequest req: requests){
            Product product = productRepository.findById(req.productId())
                    .orElseThrow(()->new ResourceNotFoundException("Product","id",req.productId()));
            result.add(VisitProduct.builder()
                    .visit(visit)
                    .product(product)
                    .samplesGiven(req.samplesGiven())
                    .feedback(req.feedback())
                    .build());
        }
        return result;
    }
}
