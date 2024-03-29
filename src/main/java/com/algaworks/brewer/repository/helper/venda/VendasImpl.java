package com.algaworks.brewer.repository.helper.venda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.dto.VendaMes;
import com.algaworks.brewer.dto.VendaOrigem;
import com.algaworks.brewer.model.StatusVenda;
import com.algaworks.brewer.model.Venda;
import com.algaworks.brewer.repository.filter.VendaFilter;
import com.algaworks.brewer.repository.paginacao.PaginacaoUtil;

public class VendasImpl implements VendasQueries {

	@Autowired
	private PaginacaoUtil paginacaoUtil;
	
	@PersistenceContext
	private EntityManager manager;
	
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	@Transactional(readOnly = true)
	public Page<Venda> filtrar(VendaFilter filter, Pageable pageable) {
		Criteria criteria           = manager.unwrap(Session.class).createCriteria(Venda.class);
		
		paginacaoUtil.preparar(criteria, pageable);
		adicionarFiltro(filter, criteria);

		return new PageImpl<>(criteria.list(), pageable, total(filter));
	}
	
	@SuppressWarnings("deprecation")
	@Transactional(readOnly = true)
	@Override
	public Venda buscarComItens(Long codigo) {
		Criteria criteria = manager.unwrap(Session.class).createCriteria(Venda.class);
		criteria.createAlias("itens", "i", JoinType.LEFT_OUTER_JOIN);
		criteria.add(Restrictions.eq("codigo", codigo));
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return (Venda) criteria.uniqueResult();
	}
	
	@Override
	public BigDecimal valorTotalNoAno() {
		Optional<BigDecimal> optional = Optional.ofNullable(
				manager.createQuery("select sum(valorTotal) from Venda where year(dataCriacao) = :ano and status = :status", BigDecimal.class)
					.setParameter("ano", Year.now().getValue())
					.setParameter("status", StatusVenda.EMITIDA)
					.getSingleResult());
		return optional.orElse(BigDecimal.ZERO);
	}

	@Override
	public BigDecimal valorTotalNoMes() {
		Optional<BigDecimal> optional = Optional.ofNullable(
				manager.createQuery("select sum(valorTotal) from Venda where month(dataCriacao) = :mes and status = :status", BigDecimal.class)
					.setParameter("mes", MonthDay.now().getMonth().getValue())
					.setParameter("status", StatusVenda.EMITIDA)
					.getSingleResult());
		return optional.orElse(BigDecimal.ZERO);
	}
	
	@Override
	public BigDecimal valorTicketMedioNoAno() {
		Optional<BigDecimal> optional = Optional.ofNullable(
				manager.createQuery("select sum(valorTotal)/count(*) from Venda where year(dataCriacao) = :ano and status = :status", BigDecimal.class)
					.setParameter("ano", Year.now().getValue())
					.setParameter("status", StatusVenda.EMITIDA)
					.getSingleResult());
		return optional.orElse(BigDecimal.ZERO);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<VendaMes> totalPorMes() {
		List<VendaMes> vendasMes = manager.createNamedQuery("Vendas.totalPorMes").getResultList();
		
		LocalDate hoje           = LocalDate.now();
		for (int i = 1; i <= 6; i++) {
			String mesIdeal      = String.format("%d/%02d", hoje.getYear(), hoje.getMonth().getValue());
			
			boolean possuiMes    = vendasMes.stream().filter(v -> v.getMes().equals(mesIdeal)).findAny().isPresent();
			if (!possuiMes) {
				vendasMes.add(i - 1, new VendaMes(mesIdeal, 0));
			}
			
			hoje                 = hoje.minusMonths(1);
		}
		
		return vendasMes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<VendaOrigem> totalPorOrigem() {
		List<VendaOrigem> vendasOrigem = manager.createNamedQuery("Vendas.porOrigem").getResultList();;
		
		LocalDate hoje           = LocalDate.now();
		for (int i = 1; i <= 6; i++) {
			String mesIdeal      = String.format("%d/%02d", hoje.getYear(), hoje.getMonth().getValue());
			
			boolean possuiMes    = vendasOrigem.stream().filter(v -> v.getMes().equals(mesIdeal)).findAny().isPresent();
			if (!possuiMes) {
				vendasOrigem.add(i - 1, new VendaOrigem(mesIdeal, 0, 0));
			}
			
			hoje                 = hoje.minusMonths(1);
		}
		
		return vendasOrigem;
	}
	
	private void adicionarFiltro(VendaFilter filter, Criteria criteria) {
		criteria.createAlias("cliente", "c");
		
		if (filter != null) {
			if (filter.getCodigo() != null) {
				criteria.add(Restrictions.eq("codigo", filter.getCodigo()));
			}
			if (filter.getStatus() != null) {
				criteria.add(Restrictions.eq("status", filter.getStatus()));
			}
			if (filter.getDataInicial() != null) {
				criteria.add(Restrictions.ge("dataCriacao", filter.getDataInicial()));
			}
			if (filter.getDataFinal() != null) {
				criteria.add(Restrictions.le("dataCriacao", filter.getDataFinal()));
			}
			if (filter.getValorInicial() != null) {
				criteria.add(Restrictions.ge("valorTotal", filter.getValorInicial()));
			}
			if (filter.getValorFinal() != null) {
				criteria.add(Restrictions.le("valorTotal", filter.getValorFinal()));
			}
			if (!StringUtils.isEmpty(filter.getNomeCliente())) {
				criteria.add(Restrictions.ilike("c.nome", filter.getNomeCliente(), MatchMode.ANYWHERE));
			}
			if (!StringUtils.isEmpty(filter.getCpfOuCnpjCliente())) {
				criteria.add(Restrictions.eq("c.cpfOuCnpj", filter.getCpfOuCnpjCliente()));
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	private Long total(VendaFilter filter) {
		Criteria criteria = manager.unwrap(Session.class).createCriteria(Venda.class);
		adicionarFiltro(filter, criteria);
		criteria.setProjection(Projections.rowCount());
		return (Long) criteria.uniqueResult();
	}
}
