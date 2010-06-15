package se.raa.ksamsok.organization;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.sql.DataSource;

import se.raa.ksamsok.harvest.DBBasedManagerImpl;
import se.raa.ksamsok.harvest.DBUtil;

/**
 * Klass f�r att hantera databas graj f�r att modda organisationers
 * information
 * @author Henrik Hjalmarsson
 */
public class OrganizationDatabaseHandler extends DBBasedManagerImpl
{

	/**
	 * Skapar en ny databashanterare
	 * @param ds datak�lla som skall anv�ndas
	 */
	public OrganizationDatabaseHandler(DataSource ds)
	{
		super(ds);
	}
	
	/**
	 * Returnerar en mapp med de organisationer som finns i databasen
	 * @return Map med String,String. Key �r kortnamn f�r organisation
	 * och value �r det svenska namnet f�r organisationen
	 */
	public List<Organization> getServiceOrganizations()
	{
		List<Organization> list = new Vector<Organization>();
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT kortnamn, serv_org, namnSwe FROM organisation";
			c = ds.getConnection();
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while(rs.next()) {
				Organization o = new Organization();
				o.setKortnamn(rs.getString("kortnamn"));
				o.setServ_org(rs.getString("serv_org"));
				o.setNamnSwe(rs.getString("namnSwe"));
				list.add(o);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return list;
	}
	
	/**
	 * Returnerar en b�na inneh�llande de v�rden som finns i databasen
	 * f�r given organisation.
	 * @param kortnamn organisationens kortnamn
	 * @return B�na med organisations-data
	 */
	public Organization getOrganization(String kortnamn, boolean isServOrg)
	{	
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Organization org = new Organization();
		try {
			String sql = "SELECT * FROM organisation WHERE kortnamn=?";
			if (isServOrg) {
				sql = "SELECT * FROM organisation WHERE serv_org=?";
			} 
			c = ds.getConnection();
			ps = c.prepareStatement(sql);
			ps.setString(1, kortnamn);
			rs = ps.executeQuery();
			if(rs.next()) {
				setOrgValues(org, rs);
				DBUtil.closeDBResources(rs, ps, null);
				rs = null;
				ps = null;
				sql = "SELECT name, beskrivning, kortnamn FROM harvestservices WHERE kortnamn= ?";
				ps = c.prepareStatement(sql);
				ps.setString(1, org.getKortnamn());
				rs = ps.executeQuery();
				List<Service> serviceList = new Vector<Service>();
				while(rs.next()) {
					Service s = new Service();
					s.setNamn(rs.getString("name"));
					s.setBeskrivning(rs.getString("beskrivning"));
					s.setKortnamn(rs.getString("kortnamn"));
					serviceList.add(s);
				}
				org.setServiceList(serviceList);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return org;
	}
	
	/**
	 * S�tter v�rden f�r organisationsb�nan
	 * @param org organisationsb�nan
	 * @param rs ResultSet fr�n SQL query
	 * @return Organisationsb�nan med satta v�rden
	 */
	private Organization setOrgValues(Organization org, ResultSet rs)
	{
		try {
			org.setKortnamn(rs.getString("kortnamn"));
			org.setServ_org(rs.getString("serv_org"));
			org.setNamnSwe(rs.getString("namnswe"));
			org.setNamnEng(rs.getString("namneng"));
			org.setBeskrivSwe(rs.getString("beskrivswe"));
			org.setBeskrivEng(rs.getString("beskriveng"));
			org.setAdress1(rs.getString("adress1"));
			org.setAdress2(rs.getString("adress2"));
			org.setPostadress(rs.getString("postadress"));
			org.setKontaktPerson(rs.getString("kontaktperson"));
			org.setEpostKontaktPerson(rs.getString("epostkontaktperson"));
			org.setWebsida(rs.getString("websida"));
			org.setWebsidaKS(rs.getString("websidaks"));
			org.setLowressUrl(rs.getString("lowressurl"));
			org.setThumbnailUrl(rs.getString("thumbnailurl"));
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return org;
	}
	
	/**
	 * Uppdaterar given organisation i databasen
	 * @param org organisationen som skall uppdateras
	 */
	public void updateOrg(Organization org)
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "UPDATE organisation SET namnswe=?, namneng=?, beskrivswe=?, beskriveng=?, adress1=?, adress2=?, postadress=?, kontaktperson=?, epostkontaktperson=?, websida=?, websidaks=?, lowressurl=?, thumbnailurl=?, serv_org=? WHERE kortnamn=?";
			ps = c.prepareStatement(sql);
			setPsStrings(ps, org);
			ps.executeUpdate();
			List<Service> serviceList = org.getServiceList();
			for(int i = 0; serviceList != null && i < serviceList.size(); i++) {
				DBUtil.closeDBResources(null, ps, null);
				ps = null;
				Service s = serviceList.get(i);
				sql = "UPDATE harvestServices SET beskrivning='" + s.getBeskrivning() + "' WHERE name='" + s.getNamn() + "'";
				ps = c.prepareStatement(sql);
				ps.executeUpdate();
			}
			DBUtil.commit(c);
		} catch (SQLException e) {
			DBUtil.rollback(c);
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
	}
	
	/**
	 * S�tter Str�ngar i SQL satsen med r�tta v�rden
	 * @param ps PreparedStatment som inneh�ller query
	 * @param org organisationens uppdaterade data
	 * @return PreparedStatement med str�ngar satta
	 */
	private PreparedStatement setPsStrings(PreparedStatement ps, Organization org)
	{
		try {
			ps.setString(1, org.getNamnSwe());
			ps.setString(2, org.getNamnEng());
			ps.setString(3, org.getBeskrivSwe());
			ps.setString(4, org.getBeskrivEng());
			ps.setString(5, org.getAdress1());
			ps.setString(6, org.getAdress2());
			ps.setString(7, org.getPostadress());
			ps.setString(8, org.getKontaktperson());
			ps.setString(9, org.getEpostKontaktperson());
			ps.setString(10, org.getWebsida());
			ps.setString(11, org.getWebsidaKS());
			ps.setString(12, org.getLowressUrl());
			ps.setString(13, org.getThumbnailUrl());
			ps.setString(14, org.getServ_org());
			ps.setString(15, org.getKortnamn());
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return ps;
	}
	
	/**
	 * Returnerar alla organisationer i databasen i form av en lista med organisationsb�nor
	 * @return Lista med organisationer i databasen
	 */
	public List<Organization> getAllOrganizations()
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<Organization> orgList = new Vector<Organization>();
		try {
			c = ds.getConnection();
			String sql = "SELECT kortnamn FROM organisation";
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while(rs.next()) {
				orgList.add(getOrganization(rs.getString("kortnamn"), false));
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return orgList;
	}
	
	/**
	 * Kollar om given anv�ndare med l�senord st�mmer.
	 * @param kortnamn organisationen som skall autensieras
	 * @param password l�senordet f�r organisationen
	 * @return true om l�senord och kortnamn �r korrekt. Annars false
	 */
	public boolean Authenticate(String kortnamn, String password)
	{
		boolean authentic = false;
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "SELECT pass FROM organisation WHERE kortnamn=?";
			ps = c.prepareStatement(sql);
			ps.setString(1, kortnamn);
			rs = ps.executeQuery();
			if(rs.next()) {
				String storedPassword = rs.getString("pass");
				if(password.equals(storedPassword)) {
					authentic = true;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return authentic;
	}
	
	/**
	 * Returnerar en Map med alla organisationskortnamn och deras l�sen
	 * @return Map med l�senord
	 */
	public Map<String,String> getPasswords()
	{
		Map<String,String> passwordMap = new HashMap<String,String>();
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "SELECT kortnamn, pass FROM organisation";
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while(rs.next()) {
				String kortnamn = rs.getString("kortnamn");
				String password = rs.getString("pass");
				passwordMap.put(kortnamn, password);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return passwordMap;
	}
	
	/**
	 * �ndrar l�senord f�r organisationer
	 * @param passwordMap Map med anv�ndare och l�senord.
	 */
	public void setPassword(Map<String,String> passwordMap)
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "UPDATE organisation SET pass=? WHERE kortnamn=?";
			for(Map.Entry<String, String> entry : passwordMap.entrySet()) {
				ps = c.prepareStatement(sql);
				ps.setString(1, entry.getValue());
				ps.setString(2, entry.getKey());
				ps.executeUpdate();
				DBUtil.closeDBResources(null, ps, null);
			}
			DBUtil.commit(c);
		} catch (SQLException e) {
			DBUtil.rollback(c);
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
	}
	
	/**
	 * L�gger till en ny organisation i databasen
	 * (�vrig info f�r fixas i efterhand)
	 * @param kortnamn Kortnamnet f�r organisationen
	 * @param namnSwe svenska namnet f�r organisationen
	 */
	public void addOrganization(String kortnamn, String namnSwe)
	{
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			String sql = "INSERT INTO organisation(kortnamn, serv_org, namnswe) VALUES(?, ?, ?)";
			ps = c.prepareStatement(sql);
			int i = 0;
			ps.setString(++i, kortnamn);
			ps.setString(++i, kortnamn);
			ps.setString(++i, namnSwe);
			ps.executeUpdate();
			DBUtil.commit(c);
		}catch(SQLException e) {
			DBUtil.rollback(c);
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}
}
