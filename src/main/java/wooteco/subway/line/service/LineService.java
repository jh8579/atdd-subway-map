package wooteco.subway.line.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wooteco.subway.line.dao.LineDao;
import wooteco.subway.line.domain.Line;
import wooteco.subway.line.domain.LineRoute;
import wooteco.subway.line.dto.LineCreateRequest;
import wooteco.subway.line.dto.LineResponse;
import wooteco.subway.line.dto.LineUpdateRequest;
import wooteco.subway.section.dao.SectionDao;
import wooteco.subway.section.domain.Section;
import wooteco.subway.station.dao.StationDao;
import wooteco.subway.station.dto.StationResponse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LineService {
    private final LineDao lineDao;
    private final StationDao stationDao;
    private final SectionDao sectionDao;

    public LineService(LineDao lineDao, StationDao stationDao, SectionDao sectionDao) {
        this.lineDao = lineDao;
        this.stationDao = stationDao;
        this.sectionDao = sectionDao;
    }

    @Transactional
    public LineResponse save(LineCreateRequest lineCreateRequest) {
        validateDuplicateName(lineCreateRequest.getName());
        validateIfDownStationIsEqualToUpStation(lineCreateRequest);

        Line line = new Line(lineCreateRequest.getName(), lineCreateRequest.getColor());
        Line savedLine = lineDao.save(line);

        stationDao.findById(lineCreateRequest.getDownStationId())
                .orElseThrow(() -> new IllegalArgumentException("입력하신 하행역이 존재하지 않습니다."));
        stationDao.findById(lineCreateRequest.getUpStationId())
                .orElseThrow(() -> new IllegalArgumentException("입력하신 상행역이 존재하지 않습니다."));
        sectionDao.save(new Section(savedLine.getId(),
                lineCreateRequest.getUpStationId(),
                lineCreateRequest.getDownStationId(),
                lineCreateRequest.getDistance()
        ));

        return new LineResponse(savedLine);
    }

    private void validateIfDownStationIsEqualToUpStation(LineCreateRequest lineCreateRequest) {
        if (lineCreateRequest.isSameStations()) {
            throw new IllegalArgumentException("상행과 하행 종점은 같을 수 없습니다.");
        }
    }

    public List<LineResponse> findAll() {
        List<Line> lines = lineDao.findAll();
        return lines.stream()
                .map(LineResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public LineResponse find(Long id) {
        Line line = lineDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 노선이 존재하지 않습니다."));
        List<Section> sectionsByLineId = sectionDao.findAllByLineId(line.getId());
        LineRoute lineRoute = new LineRoute(sectionsByLineId);
        List<StationResponse> stations = lineRoute.getOrderedStations()
                .stream()
                .map(stationDao::findById)
                .map(Optional::get)
                .map(station -> new StationResponse(station.getId(), station.getName()))
                .collect(Collectors.toList());
        return new LineResponse(line, stations);
    }

    @Transactional
    public void delete(Long id) {
        lineDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제하려는 노선이 존재하지 않습니다"));
        lineDao.delete(id);
    }

    public void update(Long id, LineUpdateRequest lineUpdateRequest) {
        lineDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정하려는 노선이 존재하지 않습니다"));
        validateDuplicateNameExceptMyself(id, lineUpdateRequest.getName());
        Line line = new Line(id, lineUpdateRequest.getName(), lineUpdateRequest.getColor());
        lineDao.update(line);
    }

    private void validateDuplicateNameExceptMyself(Long id, String lineName) {
        Optional<Line> lineByName = lineDao.findByName(lineName);
        if (lineByName.isPresent() && !lineByName.get().getId().equals(id)) {
            throw new IllegalArgumentException("같은 이름의 노선이 있습니다;");
        }
    }

    private void validateDuplicateName(String lineName) {
        if (lineDao.findByName(lineName).isPresent()) {
            throw new IllegalArgumentException("같은 이름의 노선이 있습니다;");
        }
    }

    private void validateIsExistStationById(Long id) {
        if (!stationDao.findById(id).isPresent()) {
            throw new IllegalArgumentException("해당 지하철역이 존재하지 않습니다");
        }
    }

    private void validateIsExistLineById(Long id) {
        if (!lineDao.findById(id).isPresent()) {
            throw new IllegalArgumentException("해당 노선이 존재하지 않습니다");
        }
    }

    private Line findLineById(Long id) {
        return lineDao.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 노선이 존재하지 않습니다"));
    }
}
