package com.agenciahub.api.application.usecases.quotation.shared;

import com.agenciahub.api.application.persistence.entity.*;
import com.agenciahub.api.application.persistence.repository.*;
import com.agenciahub.api.domain.QuotationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.agenciahub.api.application.services.flights.FlightPlanSelection;
import com.agenciahub.api.domain.*;
import java.util.List;

/** Runs inside the quotation transaction, with the quotation row locked before the sale. */
@Service
@RequiredArgsConstructor
public class QuotationApprovalService {
    private final SaleRepository sales;
    private final SaleItemRepository items;
    private final ReceivableRepository receivables;
    private final PayableRepository payables;
    private final TripRepository trips;
    private final FinancialEntryRepository financialEntries;

    @Transactional
    public void synchronize(Quotation quotation) {
        List<Sale> linked = sales.findByQuotation_IdOrderByCreatedAtAsc(quotation.getId());
        if (linked.size() > 1) throw new IllegalArgumentException("Há várias vendas nesta cotação. Revise os vínculos antes de alterar a aprovação.");
        Sale sale = linked.isEmpty() ? null : sales.findForUpdate(linked.get(0).getId(), quotation.getAgency().getId()).orElseThrow();
        if (quotation.getStatus() != QuotationStatus.ACCEPTED) {
            if (sale != null && !"CANCELLED".equals(sale.getStatus())) {
                assertUnsettled(sale);
                sale.setStatus("CANCELLED");
                receivables.findBySale_Id(sale.getId()).forEach(r -> { r.setStatus("CANCELLED"); if (r.getFinancialEntry() != null) r.getFinancialEntry().setStatus(FinancialEntryStatus.CANCELLED); });
                payables.findBySale_Id(sale.getId()).forEach(p -> { p.setStatus("CANCELLED"); if (p.getFinancialEntry() != null) p.getFinancialEntry().setStatus(FinancialEntryStatus.CANCELLED); });
                trips.findByQuotation_Id(quotation.getId()).forEach(t -> t.setStatus("CANCELLED"));
            }
            return;
        }
        if (!"BRL".equals(quotation.getCurrency()) || quotation.getTotalAmount().signum() <= 0)
            throw new IllegalArgumentException("Para aprovar, informe um valor maior que zero em BRL.");
        if (sale == null) {
            sale = sales.save(Sale.builder().agency(quotation.getAgency()).customer(quotation.getCustomer())
                    .quotation(quotation).approvalManaged(true).status("DRAFT").totalAmount(quotation.getTotalAmount())
                    .saleDate(LocalDate.now()).notes("Cotação: " + quotation.getTitle()).build());
            receivables.save(Receivable.builder().sale(sale).installmentNumber(1).amount(sale.getTotalAmount())
                    .dueDate(LocalDate.now()).status("PENDING").build());
            items.save(SaleItem.builder().sale(sale).description(quotation.getTitle()).itemType("OTHER")
                    .saleAmount(sale.getTotalAmount()).customerPaysSupplierDirectly(false).build());
        } else {
            boolean reopening = "CANCELLED".equals(sale.getStatus());
            if (sale.getTotalAmount().compareTo(quotation.getTotalAmount()) != 0) {
                if (!"DRAFT".equals(sale.getStatus()) && !reopening)
                    throw new IllegalArgumentException("A venda já foi confirmada. Reverta a aprovação antes de alterar o valor da cotação.");
                List<Receivable> rows = receivables.findBySale_Id(sale.getId());
                List<SaleItem> lines = items.findBySale_Id(sale.getId());
                if (rows.size() != 1 || lines.size() != 1 || Boolean.TRUE.equals(lines.get(0).getCustomerPaysSupplierDirectly()))
                    throw new IllegalArgumentException("Revise o parcelamento da venda antes de alterar o valor da cotação.");
                assertUnsettled(sale);
                sale.setTotalAmount(quotation.getTotalAmount());
                rows.get(0).setAmount(sale.getTotalAmount());
                lines.get(0).setSaleAmount(sale.getTotalAmount());
            }
            if (reopening) {
                sale.setStatus("DRAFT");
                receivables.findBySale_Id(sale.getId()).forEach(r -> r.setStatus("PENDING"));
                payables.findBySale_Id(sale.getId()).forEach(p -> p.setStatus("PENDING"));
            }
        }
        if (sale.isApprovalManaged()) synchronizeCostsAndForecast(sale, quotation);
        List<Trip> linkedTrips = trips.findByQuotation_Id(quotation.getId());
        if (linkedTrips.isEmpty()) {
            trips.save(Trip.builder().agency(quotation.getAgency()).customer(quotation.getCustomer())
                    .quotation(quotation).sale(sale).serviceType(FlightPlanSelection.option(quotation) == null ? "PACKAGE" : "FLIGHT").status("UPCOMING")
                    .saleDate(sale.getSaleDate()).travelStartDate(FlightPlanSelection.start(quotation))
                    .travelEndDate(FlightPlanSelection.end(quotation)).notes(quotation.getTitle() + " — " + quotation.getDestination()).build());
        } else for (Trip trip : linkedTrips) {
            if (trip.getSale() != null && !trip.getSale().getId().equals(sale.getId()))
                throw new IllegalArgumentException("A viagem já está vinculada a outra venda.");
            trip.setSale(sale);
            if (trip.getBookingLocator() == null || trip.getBookingLocator().isBlank()) {
                trip.setTravelStartDate(FlightPlanSelection.start(quotation));
                trip.setTravelEndDate(FlightPlanSelection.end(quotation));
            }
            if ("CANCELLED".equals(trip.getStatus())) trip.setStatus("UPCOMING");
        }
    }

    private void synchronizeCostsAndForecast(Sale sale, Quotation quotation) {
        BigDecimal cost = FlightPlanSelection.cost(quotation);
        if (cost == null) return; // Manual quotations do not imply zero supplier cost.
        List<SaleItem> lines = items.findBySale_Id(sale.getId());
        if (lines.size() != 1) throw new IllegalArgumentException("Revise os itens da venda antes de alterar os custos da cotação.");
        SaleItem line = lines.get(0);
        if (line.getSupplierCost() != null && line.getSupplierCost().compareTo(cost) != 0) assertUnsettled(sale);
        line.setSupplierCost(cost);
        line.setSaleAmount(sale.getTotalAmount());
        items.save(line);
        for (Receivable row : receivables.findBySale_Id(sale.getId())) {
            if (!"PENDING".equals(row.getStatus())) continue;
            FinancialEntry entry = row.getFinancialEntry();
            if (entry == null) entry = FinancialEntry.builder().agency(sale.getAgency()).customer(sale.getCustomer())
                    .type(FinancialEntryType.INCOME).category(FinancialEntryCategory.PACKAGE_SOLD).build();
            entry.setDescription("Recebimento previsto — " + quotation.getTitle());
            entry.setAmount(row.getAmount()); entry.setEntryDate(row.getDueDate());
            entry.setStatus(FinancialEntryStatus.PENDING); entry.setSaleAmount(row.getAmount());
            entry.setSupplierCost(cost.multiply(row.getAmount()).divide(sale.getTotalAmount(),2,java.math.RoundingMode.HALF_UP)); entry.setCommissionAmount(BigDecimal.ZERO);
            row.setFinancialEntry(financialEntries.save(entry)); receivables.save(row);
        }
        List<Payable> costs = payables.findBySale_Id(sale.getId()).stream().filter(p -> "SUPPLIER".equals(p.getPayableType())).toList();
        if (costs.isEmpty() && receivables.findBySale_Id(sale.getId()).stream().anyMatch(r -> "PAID".equals(r.getStatus()))) return;
        if (costs.size() > 1) throw new IllegalArgumentException("Revise os custos da venda antes de alterar a cotação.");
        Payable payable = costs.isEmpty() ? null : costs.get(0);
        if (payable == null && cost.signum() > 0) payable = Payable.builder().sale(sale).payableType("SUPPLIER").dueDate(LocalDate.now()).status("PENDING").build();
        if (payable != null && !"PAID".equals(payable.getStatus())) {
            payable.setAmount(cost); payable.setStatus(cost.signum() == 0 ? "CANCELLED" : "PENDING");
            FinancialEntry entry = payable.getFinancialEntry();
            if (entry == null && cost.signum() > 0) entry = FinancialEntry.builder().agency(sale.getAgency()).customer(sale.getCustomer())
                    .type(FinancialEntryType.EXPENSE).category(FinancialEntryCategory.OTHER).build();
            if (entry != null) {
                entry.setDescription("Custo previsto — " + quotation.getTitle()); entry.setAmount(cost);
                entry.setEntryDate(payable.getDueDate()); entry.setStatus(cost.signum() == 0 ? FinancialEntryStatus.CANCELLED : FinancialEntryStatus.PENDING);
                payable.setFinancialEntry(financialEntries.save(entry));
            }
            payables.save(payable);
        }
    }

    private void assertUnsettled(Sale sale) {
        if (receivables.findBySale_Id(sale.getId()).stream().anyMatch(r -> "PAID".equals(r.getStatus()))
                || payables.findBySale_Id(sale.getId()).stream().anyMatch(p -> "PAID".equals(p.getStatus())))
            throw new IllegalArgumentException("Estorne os recebimentos e pagamentos da venda antes de reverter a aprovação.");
    }
}
